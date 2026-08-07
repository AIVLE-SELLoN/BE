package com.aivle.sellon.domain.report.service;

import com.aivle.sellon.domain.mypage.entity.MonthlyReportSetting;
import com.aivle.sellon.domain.mypage.repository.MonthlyReportRecipientRepository;
import com.aivle.sellon.domain.mypage.repository.MonthlyReportSettingRepository;
import com.aivle.sellon.domain.report.entity.Report;
import com.aivle.sellon.domain.report.entity.ReportMailDelivery;
import com.aivle.sellon.domain.report.enums.ReportMailDeliveryStatus;
import com.aivle.sellon.domain.report.repository.ReportMailDeliveryRepository;
import com.aivle.sellon.domain.report.repository.ReportRepository;
import com.aivle.sellon.domain.user.entity.User;
import com.aivle.sellon.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * 월간 보고서 완료 메일의 예약 생성과 발송 처리.
 * 큐 수신 시점에는 예약 행만 만들고, 실제 발송은 스케줄러가 예약 시각에 수행한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReportMailDeliveryService {

    private final ReportRepository reportRepository;
    private final ReportMailDeliveryRepository deliveryRepository;
    private final MonthlyReportSettingRepository settingRepository;
    private final MonthlyReportRecipientRepository recipientRepository;
    private final UserRepository userRepository;
    private final ReportDownloadUrlService reportDownloadUrlService;
    private final ReportMailSender mailSender;

    /**
     * 수신자 1명당 예약 1건을 만든다. 같은 메시지가 재전달돼도
     * (report_id, email)이 이미 있으면 건너뛰므로 중복 발송되지 않는다.
     */
    @Transactional
    public void scheduleFor(Long reportId, Long companyId) {
        MonthlyReportSetting setting = settingRepository.findByCompanyIdAndDeletedAtIsNull(companyId).orElse(null);
        if (setting != null && !setting.isEnabled()) {
            log.info("월간 보고서 메일 예약 생략 - 수신 설정 꺼짐. companyId={}", companyId);
            return;
        }

        Report report = reportRepository.findById(reportId).orElse(null);
        if (report == null) {
            log.warn("월간 보고서 메일 예약 실패 - 리포트 없음. reportId={}", reportId);
            return;
        }

        Set<String> emails = collectRecipients(companyId);
        if (emails.isEmpty()) {
            log.warn("월간 보고서 메일 예약 생략 - 수신자 없음. companyId={}", companyId);
            return;
        }

        emails.removeAll(Set.copyOf(deliveryRepository.findScheduledEmails(reportId)));
        if (emails.isEmpty()) {
            log.info("월간 보고서 메일 예약 생략 - 이미 예약됨(재전달). reportId={}", reportId);
            return;
        }

        LocalDateTime scheduledAt = resolveScheduledAt(setting, LocalDateTime.now());
        emails.forEach(email -> deliveryRepository.save(ReportMailDelivery.schedule(report, email, scheduledAt)));

        log.info("월간 보고서 메일 예약 완료 - {}건, scheduledAt={}, reportId={}", emails.size(), scheduledAt, reportId);
    }

    /**
     * 예약 1건을 발송한다. 실패해도 예외를 밖으로 내보내지 않고 상태로만 남겨,
     * 같은 배치의 다른 예약이 함께 롤백되지 않게 한다.
     */
    @Transactional
    public void process(Long deliveryId) {
        ReportMailDelivery delivery = deliveryRepository.findById(deliveryId).orElse(null);
        if (delivery == null || delivery.getStatus() != ReportMailDeliveryStatus.PENDING)
            return;

        Report report = delivery.getReport();

        // object_expires_at이 지나 S3에서 지워진 경우. 월간은 재생성 경로가 없어 재시도해도 결과가 같다.
        String downloadUrl = reportDownloadUrlService.generate(report.getPdfS3Meta());
        if (downloadUrl == null) {
            delivery.giveUp("다운로드 가능한 PDF 없음");
            log.warn("월간 보고서 메일 포기 - PDF 없음. deliveryId={}, reportId={}", deliveryId, report.getId());
            return;
        }

        try {
            mailSender.send(delivery.getEmail(), report, downloadUrl);
            delivery.markSent();
            log.info("월간 보고서 메일 발송 완료. deliveryId={}, reportId={}, attempt={}",
                    deliveryId, report.getId(), delivery.getAttemptCount());
        } catch (Exception e) {
            delivery.markFailed(e.getMessage());
            log.error("월간 보고서 메일 발송 실패. deliveryId={}, reportId={}, attempt={}/{}, status={}",
                    deliveryId, report.getId(), delivery.getAttemptCount(),
                    ReportMailDelivery.MAX_ATTEMPTS, delivery.getStatus(), e);
        }
    }

    private Set<String> collectRecipients(Long companyId) {
        Set<String> emails = new LinkedHashSet<>();

        userRepository.findRootByCompanyIdAndDeletedAtIsNull(companyId)
                .map(User::getEmail)
                .ifPresent(emails::add);

        recipientRepository.findAllByCompanyIdAndDeletedAtIsNullOrderByIdAsc(companyId)
                .forEach(recipient -> emails.add(recipient.getEmail()));

        return emails;
    }

    /**
     * 설정이 없으면 즉시 발송한다. 설정이 있으면 이번 달 sendDay/sendTime에 맞추되,
     * 그 시각이 이미 지났으면(예: 이벤트가 sendDay 이후에 도착) 미루지 않고 즉시 보낸다.
     */
    private LocalDateTime resolveScheduledAt(MonthlyReportSetting setting, LocalDateTime now) {
        if (setting == null)
            return now;

        LocalDateTime scheduledAt = LocalDateTime.of(
                now.toLocalDate().withDayOfMonth(setting.getSendDay()), setting.getSendTime());

        return scheduledAt.isBefore(now) ? now : scheduledAt;
    }
}

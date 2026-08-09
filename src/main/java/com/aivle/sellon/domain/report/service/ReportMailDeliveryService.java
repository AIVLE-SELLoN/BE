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
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

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

     * REQUIRES_NEW인 이유: 이 메서드는 AFTER_COMMIT 콜백에서 호출되는데, 그 시점에는 커밋이 끝났어도
     * EntityManager가 아직 스레드에 바인딩돼 있다(정리는 afterCompletion에서 일어난다).
     * 기본값인 REQUIRED로 두면 Spring이 "트랜잭션이 있다"고 판단해 이미 끝난 트랜잭션에 조인하고,
     * 저장이 커밋되지 않은 채 예외도 없이 사라진다.
     *
     * 리포트 행을 락으로 잡는 이유: 쿠버네티스 환경에서는 같은 report_id에 대한 메시지(원본+재전달)가
     * 서로 다른 인스턴스에서 동시에 처리될 수 있다. 락 없이 기존 수신자를 조회하면 두 트랜잭션이
     * 같은 스냅샷을 보고 각자 INSERT를 시도하다 유니크 제약(report_id, email) 충돌이 나고,
     * 그 배치 안에 함께 있던 진짜 신규 수신자까지 롤백으로 유실된다. 락을 걸면 뒤에 온 트랜잭션은
     * 앞선 트랜잭션이 커밋할 때까지 대기했다가 최신 상태를 다시 읽으므로 이 경합이 사라진다.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void scheduleFor(Long reportId, Long companyId) {
        MonthlyReportSetting setting = settingRepository.findByCompanyIdAndDeletedAtIsNull(companyId).orElse(null);
        if (setting != null && !setting.isEnabled()) {
            log.info("월간 보고서 메일 예약 생략 - 수신 설정 꺼짐. companyId={}", companyId);
            return;
        }

        Report report = reportRepository.findByIdForUpdate(reportId).orElse(null);
        if (report == null) {
            log.warn("월간 보고서 메일 예약 실패 - 리포트 없음. reportId={}", reportId);
            return;
        }

        Set<String> emails = collectRecipients(companyId);
        if (emails.isEmpty()) {
            log.warn("월간 보고서 메일 예약 생략 - 수신자 없음. companyId={}", companyId);
            return;
        }

        Map<String, ReportMailDelivery> existing = deliveryRepository.findAllByReportId(reportId).stream()
                .collect(Collectors.toMap(ReportMailDelivery::getEmail, Function.identity(), (first, ignored) -> first));

        LocalDateTime scheduledAt = resolveScheduledAt(setting, LocalDateTime.now());
        int scheduled = 0;

        for (String email : emails) {
            ReportMailDelivery previous = existing.get(email);

            if (previous == null) {
                deliveryRepository.save(ReportMailDelivery.schedule(report, email, scheduledAt));
                scheduled++;
                continue;
            }

            // 포기했던 수신자는 다시 살린다. 같은 리포트가 재전달됐다는 건 내용이 고쳐졌다는 뜻이라,
            // 이전 포기 사유(PDF 없음 등)가 그대로일 거라고 볼 이유가 없다.
            if (previous.getStatus() == ReportMailDeliveryStatus.GIVEN_UP) {
                previous.reschedule(scheduledAt);
                scheduled++;
            }
        }

        if (scheduled == 0) {
            log.info("월간 보고서 메일 예약 생략 - 이미 예약됨(재전달). reportId={}", reportId);
            return;
        }

        log.info("월간 보고서 메일 예약 완료 - {}건, scheduledAt={}, reportId={}", scheduled, scheduledAt, reportId);
    }

    /**
     * 예약 1건을 발송한다. 실패해도 예외를 밖으로 내보내지 않고 상태로만 남겨 같은 배치의 다른 예약이 함께 롤백되지 않게 한다.

     * 발송에 이르는 모든 단계를 try로 감싼다. 메일 전송뿐 아니라 presigned URL 발급도
     * 외부 호출이라 실패할 수 있는데, 이게 밖으로 새면 트랜잭션이 롤백돼 시도 횟수가
     * 기록되지 않고 크론 주기마다 같은 행을 무한히 다시 집게 된다.
     */
    @Transactional
    public void process(Long deliveryId) {
        // 락을 잡은 인스턴스만 통과한다. 못 잡으면 다른 인스턴스가 이미 발송 중이라는 뜻이다.
        ReportMailDelivery delivery = deliveryRepository.findPendingForUpdate(deliveryId).orElse(null);
        if (delivery == null)
            return;

        try {
            Report report = delivery.getReport();

            String skipReason = resolveSkipReason(report, delivery.getEmail());
            if (skipReason != null) {
                delivery.giveUp(skipReason);
                log.info("월간 보고서 메일 포기 - {}. deliveryId={}, reportId={}",
                        skipReason, deliveryId, report.getId());
                return;
            }

            // object_expires_at이 지나 S3에서 지워진 경우. 월간은 재생성 경로가 없어 재시도해도 결과가 같다.
            String downloadUrl = reportDownloadUrlService.generate(report.getPdfS3Meta());
            if (downloadUrl == null) {
                delivery.giveUp("다운로드 가능한 PDF 없음");
                log.warn("월간 보고서 메일 포기 - PDF 없음. deliveryId={}, reportId={}", deliveryId, report.getId());
                return;
            }

            mailSender.send(delivery.getEmail(), report, downloadUrl);
            delivery.markSent();
            log.info("월간 보고서 메일 발송 완료. deliveryId={}, reportId={}, attempt={}",
                    deliveryId, report.getId(), delivery.getAttemptCount());
        } catch (Exception e) {
            delivery.markFailed(e.getMessage());
            log.error("월간 보고서 메일 발송 실패. deliveryId={}, attempt={}/{}, status={}",
                    deliveryId, delivery.getAttemptCount(),
                    ReportMailDelivery.MAX_ATTEMPTS, delivery.getStatus(), e);
        }
    }

    /**
     * {@link #process}의 트랜잭션이 통째로 롤백돼 실패가 기록되지 못한 경우를 위한 보루.
     * 시도 횟수가 늘지 않으면 예약 시각도 밀리지 않아 크론 주기마다 같은 행이 계속 잡히고,
     * 배치가 오래된 실패 건으로 채워져 뒤따르는 예약이 굶는다.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordFailure(Long deliveryId, String error) {
        deliveryRepository.findById(deliveryId)
                .filter(delivery -> delivery.getStatus() == ReportMailDeliveryStatus.PENDING)
                .ifPresent(delivery -> delivery.markFailed(error));
    }

    /**
     * 예약은 sendDay까지 몇 주씩 PENDING으로 남아 있을 수 있다. 그 사이 수신 설정이 꺼지거나
     * 수신자가 빠질 수 있으므로, 예약 시점의 스냅샷을 믿지 않고 발송 직전에 다시 확인한다.
     *
     * @return 보내면 안 되는 사유. 보내도 되면 null
     */
    private String resolveSkipReason(Report report, String email) {
        Long companyId = report.getCompany().getId();

        MonthlyReportSetting setting = settingRepository.findByCompanyIdAndDeletedAtIsNull(companyId).orElse(null);
        if (setting != null && !setting.isEnabled())
            return "수신 설정 꺼짐";

        if (!collectRecipients(companyId).contains(email))
            return "수신자 목록에서 제외됨";

        return null;
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

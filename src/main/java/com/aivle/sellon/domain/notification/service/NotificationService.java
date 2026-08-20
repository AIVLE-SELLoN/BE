package com.aivle.sellon.domain.notification.service;

import com.aivle.sellon.domain.company.entity.Company;
import com.aivle.sellon.domain.company.repository.CompanyRepository;
import com.aivle.sellon.domain.notification.entity.Notification;
import com.aivle.sellon.domain.notification.repository.NotificationRepository;
import com.aivle.sellon.domain.report.entity.Report;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;

/**
 * 타 도메인(모듈 경계를 넘는) 알림 생성 전용 서비스.
 * 자기 도메인(이상탐지)은 AlertIngestService가 같은 트랜잭션에서 직접 저장하므로 여기를 거치지 않는다.
 * <p>
 * 전부 REQUIRES_NEW인 이유: 이 메서드들은 AFTER_COMMIT 리스너에서 호출된다. 그 시점엔
 * 원본 트랜잭션이 이미 끝나 있어서, 기본 전파(REQUIRED)로 두면 죽은 트랜잭션에 조인해
 * save가 커밋 없이 조용히 사라진다.
 */
@Service
@RequiredArgsConstructor
public class NotificationService {

    // AI팀 스키마(메시지 큐 컨벤션 §4.2) 확정 포맷: report_month = "YYYY-MM"
    private static final DateTimeFormatter REPORT_MONTH_INPUT = DateTimeFormatter.ofPattern("yyyy-MM");
    private static final DateTimeFormatter REPORT_MONTH_DISPLAY = DateTimeFormatter.ofPattern("yyyy년 M월");

    private final NotificationRepository notificationRepository;
    private final CompanyRepository companyRepository;

    /**
     * getReferenceById로 프록시만 받는다. 이 시점에 필요한 건 FK 값뿐이라
     * Company 전체를 다시 SELECT할 이유가 없다.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void createForMonthlyReport(Long companyId, Long reportId, String reportMonth) {
        Company companyRef = companyRepository.getReferenceById(companyId);
        String message = "%s 채널 운영 리포트를 확인하고 성과를 분석해보세요".formatted(toDisplayMonth(reportMonth));

        notificationRepository.save(
                Notification.createForMonthlyReport(companyRef, message, LocalDateTime.now(), reportId)
        );
    }

    // "2026-07" -> "7월". 파싱 실패해도 알림 생성 자체를 막을 이유는 없어 원본值을 그대로 쓴다.
    private String toDisplayMonth(String reportMonth) {
        try {
            return YearMonth.parse(reportMonth, REPORT_MONTH_INPUT).format(REPORT_MONTH_DISPLAY);
        } catch (Exception e) {
            return reportMonth;
        }
    }
}

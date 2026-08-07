package com.aivle.sellon.domain.report.service;

import com.aivle.sellon.domain.report.event.ReportGeneratedEvent;
import com.aivle.sellon.domain.user.entity.User;
import com.aivle.sellon.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReportMailService {

    private final JavaMailSender mailSender;
    private final UserRepository userRepository;
    private final ReportDownloadUrlService reportDownloadUrlService;

    @Value("${spring.mail.from}")
    private String fromAddress;

    /**
     * 커밋 이후에 발송한다. 트랜잭션 안에서 보내면 메일 실패가 리포트 저장까지 롤백시키고,
     * 재전달된 메시지가 메일을 중복 발송하게 된다.
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onReportGenerated(ReportGeneratedEvent event) {
        String downloadUrl = reportDownloadUrlService.generate(event.pdfS3Meta());
        if (downloadUrl == null) {
            log.warn("월간 보고서 메일 발송 생략 - 다운로드 가능한 PDF 없음. companyId={}, reportMonth={}",
                    event.companyId(), event.reportMonth());
            return;
        }

        Optional<User> recipient = userRepository.findRootByCompanyIdAndDeletedAtIsNull(event.companyId());
        if (recipient.isEmpty()) {
            log.warn("월간 보고서 메일 발송 실패 - 루트 계정 없음. companyId={}", event.companyId());
            return;
        }

        send(recipient.get().getEmail(), event, downloadUrl);
    }

    private void send(String email, ReportGeneratedEvent event, String downloadUrl) {
        try {
            mailSender.send(buildMessage(email, event, downloadUrl));
        } catch (MailException e) {
            // 메일 실패로 메시지를 재처리하면 리포트가 다시 저장되고 메일도 중복 발송된다
            log.error("월간 보고서 메일 발송 실패. companyId={}, reportMonth={}",
                    event.companyId(), event.reportMonth(), e);
        }
    }

    private SimpleMailMessage buildMessage(String email, ReportGeneratedEvent event, String downloadUrl) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromAddress);
        message.setTo(email);
        message.setSubject("[SELLoN] %s 월간 보고서가 준비되었습니다".formatted(event.reportMonth()));
        message.setText(buildBody(event, downloadUrl));
        return message;
    }

    private String buildBody(ReportGeneratedEvent event, String downloadUrl) {
        return """
                %s 월간 보고서 작성이 완료되었습니다.
                아래 링크에서 파일을 다운로드해주세요.

                %s
                %s""".formatted(event.reportMonth(), event.pdfS3Meta().getOriginalFileName(), downloadUrl);
    }
}

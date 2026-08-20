package com.aivle.sellon.domain.report.service;

import com.aivle.sellon.domain.report.entity.Report;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

/**
 * 월간 보고서 완료 메일의 본문 구성과 실제 발송만 담당한다.
 * 실패는 호출부가 재시도 판단에 쓰도록 그대로 던진다.
 */
@Component
@RequiredArgsConstructor
public class ReportMailSender {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.from}")
    private String fromAddress;

    public void send(String email, Report report, String downloadUrl) {
        mailSender.send(buildMessage(email, report, downloadUrl));
    }

    private SimpleMailMessage buildMessage(String email, Report report, String downloadUrl) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromAddress);
        message.setTo(email);
        message.setSubject("[SELLoN] %s 월간 보고서가 준비되었습니다".formatted(report.getReportMonth()));
        message.setText(buildBody(report, downloadUrl));
        return message;
    }

    private String buildBody(Report report, String downloadUrl) {
        StringBuilder body = new StringBuilder("""
                %s 월간 보고서 작성이 완료되었습니다.
                아래 링크에서 파일을 다운로드해주세요.

                %s
                %s
                """.formatted(
                report.getReportMonth(),
                report.getPdfS3Meta().getOriginalFileName(),
                downloadUrl));

        // 큐 컨벤션 §4.2 — SUCCESS여도 합본에서 빠진 상품이 있으면 notice_message가 채워진다
        if (report.getNoticeMessage() != null && !report.getNoticeMessage().isBlank())
            body.append("\n").append(report.getNoticeMessage()).append("\n");

        return body.toString();
    }
}

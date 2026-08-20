package com.aivle.sellon.domain.guideline.service;

import com.aivle.sellon.domain.guideline.entity.Guideline;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

/**
 * CS 가이드라인 안내 메일의 본문 구성과 실제 발송만 담당한다.
 */
@Component
@RequiredArgsConstructor
public class GuidelineMailSender {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.from}")
    private String fromAddress;

    public void send(String email, Guideline guideline, String downloadUrl) {
        mailSender.send(buildMessage(email, guideline, downloadUrl));
    }

    private SimpleMailMessage buildMessage(String email, Guideline guideline, String downloadUrl) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromAddress);
        message.setTo(email);
        message.setSubject("[SELLoN] CS 가이드라인이 도착했습니다");
        message.setText(buildBody(guideline, downloadUrl));
        return message;
    }

    private String buildBody(Guideline guideline, String downloadUrl) {
        StringBuilder body = new StringBuilder("""
                CS 가이드라인이 도착했습니다.
                아래 링크에서 파일을 다운로드해주세요.

                %s
                %s
                """.formatted(
                guideline.getPdfS3Meta().getOriginalFileName(),
                downloadUrl));

        if (guideline.getNoticeMessage() != null && !guideline.getNoticeMessage().isBlank())
            body.append("\n").append(guideline.getNoticeMessage()).append("\n");

        return body.toString();
    }
}

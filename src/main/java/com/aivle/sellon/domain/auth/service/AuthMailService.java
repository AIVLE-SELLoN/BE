package com.aivle.sellon.domain.auth.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/** 비밀번호 찾기 시 발급되는 임시 비밀번호 안내 메일. */
@Service
@RequiredArgsConstructor
public class AuthMailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.from}")
    private String fromAddress;

    public void sendTempPassword(String email, String tempPassword) {
        mailSender.send(buildMessage(email, tempPassword));
    }

    private SimpleMailMessage buildMessage(String email, String tempPassword) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromAddress);
        message.setTo(email);
        message.setSubject("[SELLoN] 임시 비밀번호 안내");
        message.setText(
                "요청하신 임시 비밀번호는 [" + tempPassword + "] 입니다.\n"
                        + "로그인 후 마이페이지에서 반드시 비밀번호를 변경해주세요."
        );
        return message;
    }
}

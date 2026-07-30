package com.aivle.sellon.domain.verification.service;

import com.aivle.sellon.domain.verification.exception.EmailNotVerifiedException;
import com.aivle.sellon.domain.verification.exception.ExpiredVerificationCodeException;
import com.aivle.sellon.domain.verification.exception.InvalidVerificationCodeException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EmailVerificationService {

    private static final String CODE_KEY_PREFIX = "email-verification:";
    private static final String TOKEN_KEY_PREFIX = "email-verify-token:";
    private static final int CODE_LENGTH = 6;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final JavaMailSender mailSender;
    private final RedisTemplate<String, Object> redisTemplate;

    @Value("${spring.mail.from}")
    private String fromAddress;

    @Value("${spring.verification.code.expire}")
    private long codeExpireMs;

    @Value("${spring.verification.token.expire}")
    private long tokenExpireMs;

    public void sendVerificationCode(String email) {
        String code = generateCode();

        redisTemplate.opsForValue().set(CODE_KEY_PREFIX + email, code, Duration.ofMillis(codeExpireMs));

        mailSender.send(buildMessage(email, code));
    }

    public String verifyCode(String email, String code) {
        String key = CODE_KEY_PREFIX + email;
        Object savedCode = redisTemplate.opsForValue().get(key);

        if (savedCode == null)
            throw new ExpiredVerificationCodeException();

        if (!savedCode.equals(code))
            throw new InvalidVerificationCodeException();

        redisTemplate.delete(key);

        String token = UUID.randomUUID().toString();
        redisTemplate.opsForValue().set(TOKEN_KEY_PREFIX + token, email, Duration.ofMillis(tokenExpireMs));
        return token;
    }

    public void validateVerificationToken(String email, String token) {
        String key = TOKEN_KEY_PREFIX + token;
        Object savedEmail = redisTemplate.opsForValue().get(key);

        if (savedEmail == null || !savedEmail.equals(email))
            throw new EmailNotVerifiedException();

        redisTemplate.delete(key);
    }

    private SimpleMailMessage buildMessage(String email, String code) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromAddress);
        message.setTo(email);
        message.setSubject("[SELLoN] 이메일 인증번호 안내");
        message.setText("인증번호는 [" + code + "] 입니다. 5분 이내에 입력해주세요.");
        return message;
    }

    private String generateCode() {
        int number = RANDOM.nextInt((int) Math.pow(10, CODE_LENGTH));
        return String.format("%0" + CODE_LENGTH + "d", number);
    }
}

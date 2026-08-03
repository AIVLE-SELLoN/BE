package com.aivle.sellon.domain.report.service;

import com.aivle.sellon.domain.report.entity.Report;
import com.aivle.sellon.domain.user.entity.User;
import com.aivle.sellon.domain.user.exception.UserNotFoundException;
import com.aivle.sellon.domain.user.repository.UserRepository;
import com.aivle.sellon.global.file.service.S3PresignedUrlService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReportMailService {

    private final JavaMailSender mailSender;
    private final UserRepository userRepository;
    private final S3PresignedUrlService s3PresignedUrlService;

    @Value("${spring.mail.from}")
    private String fromAddress;

    @Value("${report.download-url.expire}")
    private long downloadUrlExpireMs;

    public void sendCompletionMail(List<Report> reports) {
        if (reports.isEmpty())
            return;

        Long companyId = reports.getFirst().getCompany().getId();
        User recipient = userRepository.findRootByCompanyIdAndDeletedAtIsNull(companyId)
                .orElseThrow(UserNotFoundException::new);

        mailSender.send(buildMessage(recipient.getEmail(), reports));
    }

    private SimpleMailMessage buildMessage(String email, List<Report> reports) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromAddress);
        message.setTo(email);
        message.setSubject("[SELLoN] 월간 보고서 작성이 완료되었습니다");
        message.setText(buildBody(reports));
        return message;
    }

    private String buildBody(List<Report> reports) {
        Duration expiration = Duration.ofMillis(downloadUrlExpireMs);

        StringBuilder body = new StringBuilder("월간 보고서 작성이 완료되었습니다. 아래 링크에서 파일을 다운로드해주세요.\n\n");
        for (Report report : reports) {
            String downloadUrl = s3PresignedUrlService.generateDownloadUrl(report.getStoredFileName(), expiration);
            body.append(report.getOriginalFileName()).append(" : ").append(downloadUrl).append("\n");
        }

        return body.toString();
    }
}

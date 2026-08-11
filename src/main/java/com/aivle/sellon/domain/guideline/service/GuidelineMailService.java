package com.aivle.sellon.domain.guideline.service;

import com.aivle.sellon.domain.guideline.entity.Guideline;
import com.aivle.sellon.domain.guideline.exception.GuidelineDownloadUnavailableException;
import com.aivle.sellon.domain.guideline.exception.GuidelineMailSendFailedException;
import com.aivle.sellon.domain.guideline.exception.GuidelineNoCsRecipientException;
import com.aivle.sellon.domain.guideline.exception.GuidelineNotFoundException;
import com.aivle.sellon.domain.guideline.repository.GuidelineRepository;
import com.aivle.sellon.domain.mypage.entity.MonthlyReportRecipient;
import com.aivle.sellon.domain.mypage.enums.RecipientDepartment;
import com.aivle.sellon.domain.mypage.repository.MonthlyReportRecipientRepository;
import com.aivle.sellon.global.security.principal.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 화면의 "메일 전송" 버튼으로 트리거되는 CS 가이드라인 안내 메일.
 * 큐 수신 흐름과 무관하게, 요청 시점에 유효한 다운로드 링크가 있을 때만 CS 담당자에게 즉시 발송한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GuidelineMailService {

    private final GuidelineRepository guidelineRepository;
    private final MonthlyReportRecipientRepository recipientRepository;
    private final GuidelineDownloadUrlService guidelineDownloadUrlService;
    private final GuidelineMailSender mailSender;

    public void sendMail(UserPrincipal principal, String guidelineId) {
        Guideline guideline = guidelineRepository.findByCompanyIdAndGuidelineId(principal.getCompanyId(), guidelineId)
                .orElseThrow(GuidelineNotFoundException::new);

        String downloadUrl = guidelineDownloadUrlService.generate(guideline.getPdfS3Meta());
        if (downloadUrl == null)
            throw new GuidelineDownloadUnavailableException();

        List<MonthlyReportRecipient> csRecipients = recipientRepository
                .findAllByCompanyIdAndDepartmentAndDeletedAtIsNullOrderByIdAsc(
                        principal.getCompanyId(), RecipientDepartment.CS);
        if (csRecipients.isEmpty())
            throw new GuidelineNoCsRecipientException();

        sendToAll(csRecipients, guideline, downloadUrl);
    }

    /**
     * 수신자 한 명의 발송 실패가 이미 성공한 나머지 발송까지 예외로 되돌리지 않도록 건별로 격리한다.
     * 전원 실패했을 때만 실패로 취급해, 사용자가 버튼을 재시도할 때 이미 받은 사람에게 또 보내는 걸 줄인다.
     */
    private void sendToAll(List<MonthlyReportRecipient> recipients, Guideline guideline, String downloadUrl) {
        boolean anySucceeded = false;

        for (MonthlyReportRecipient recipient : recipients) {
            try {
                mailSender.send(recipient.getEmail(), guideline, downloadUrl);
                anySucceeded = true;
            } catch (Exception e) {
                log.error("CS 가이드라인 메일 발송 실패. guidelineId={}, email={}",
                        guideline.getGuidelineId(), recipient.getEmail(), e);
            }
        }

        if (!anySucceeded)
            throw new GuidelineMailSendFailedException();
    }
}

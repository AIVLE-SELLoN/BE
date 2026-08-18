package com.aivle.sellon.domain.guideline.service;

import com.aivle.sellon.domain.guideline.entity.Guideline;
import com.aivle.sellon.domain.guideline.entity.GuidelineApproval;
import com.aivle.sellon.domain.guideline.exception.GuidelineNotFoundException;
import com.aivle.sellon.domain.guideline.repository.GuidelineApprovalRepository;
import com.aivle.sellon.domain.guideline.repository.GuidelineRepository;
import com.aivle.sellon.global.security.principal.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 상세 페이지에서 운영 MD가 가이드라인을 승인하며 남기는 코멘트 저장.
 * 이미 승인된 건에 다시 호출하면 코멘트만 덮어쓴다(재승인).
 */
@Service
@RequiredArgsConstructor
public class GuidelineApprovalService {

    private final GuidelineRepository guidelineRepository;
    private final GuidelineApprovalRepository guidelineApprovalRepository;

    @Transactional
    public void approve(UserPrincipal principal, String guidelineId, String comment) {
        Guideline guideline = guidelineRepository.findByCompanyIdAndGuidelineId(principal.getCompanyId(), guidelineId)
                .orElseThrow(GuidelineNotFoundException::new);

        guidelineApprovalRepository.findByGuidelineId(guideline.getId())
                .ifPresentOrElse(
                        existing -> existing.updateComment(comment),
                        () -> guidelineApprovalRepository.save(GuidelineApproval.create(guideline, comment))
                );
    }
}

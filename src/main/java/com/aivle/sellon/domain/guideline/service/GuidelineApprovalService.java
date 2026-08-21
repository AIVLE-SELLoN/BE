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

        // 코멘트는 FE에서 선택 입력이라 request에 아예 안 실려서 null로 들어올 수 있는데,
        // comment 컬럼이 NOT NULL이라 그대로 저장하면 제약 위반으로 승인 자체가 실패한다.
        String safeComment = comment != null ? comment : "";

        guidelineApprovalRepository.findByGuidelineId(guideline.getId())
                .ifPresentOrElse(
                        existing -> existing.updateComment(safeComment),
                        () -> guidelineApprovalRepository.save(GuidelineApproval.create(guideline, safeComment))
                );
    }
}

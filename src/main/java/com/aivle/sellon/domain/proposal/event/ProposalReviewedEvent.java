package com.aivle.sellon.domain.proposal.event;

import com.aivle.sellon.domain.proposal.enums.HitlStatus;

import java.time.LocalDateTime;

// mq_events.md §8 확정 스펙 그대로: 최상위 4개 필드 + hitl_feedback 중첩 object.
// 리포트 전문(요약/확신도/근거 등)은 이 문서에 없어 뺐다 — 필요해지면 AI 팀과
// 필드 추가를 정식 협의해 문서에 반영한 뒤 다시 넣을 것.
// 이 레코드는 서비스 내부 도메인 이벤트이고, 실제 와이어 포맷(snake_case, hitlStatus는
// 한글 문자열)으로 바꾸는 건 RabbitProposalReviewEventPublisher가 담당한다 — 여기서
// Jackson 직렬화 어노테이션을 붙이면 HitlStatus enum이 그대로 "APPROVED"로 나가버려서 계약과 안 맞다.
public record ProposalReviewedEvent(
    String recommendationId,
    String alertId,
    HitlStatus hitlStatus,
    HitlFeedback hitlFeedback
) {
    public record HitlFeedback(
        LocalDateTime processedAt,
        String processedBy,
        RejectionReason rejectionReason,
        String editedText
    ) {}

    // 승인(수정 없이)일 때는 사유가 없으므로 rejectionReason 자체가 null.
    public record RejectionReason(
        String reasonCode,
        String reasonText
    ) {}
}

package com.aivle.sellon.domain.proposal.enums;

// mq_events.md §9 — 매핑 필요 없는 enum. JSON 값은 소문자 snake_case(copy_draft/image_guide)라
// Jackson name 매칭은 안 되므로 payload record는 원문 문자열로 받고 fromJson()으로 변환한다.
public enum ProposalType {
    COPY_DRAFT,
    IMAGE_GUIDE;

    public static ProposalType fromJson(String value) {
        return switch (value) {
            case "copy_draft" -> COPY_DRAFT;
            case "image_guide" -> IMAGE_GUIDE;
            default -> throw new IllegalArgumentException("알 수 없는 proposal type 값: " + value);
        };
    }
}

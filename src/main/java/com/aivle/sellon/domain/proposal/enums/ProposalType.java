package com.aivle.sellon.domain.proposal.enums;

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

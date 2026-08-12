package com.aivle.sellon.domain.channels.dto.response;

public record MatchCandidateResponse(
        Long masterProductKey,
        String masterSku,
        String productName,
        double similarityScore,
        boolean topRecommendation
) {
}

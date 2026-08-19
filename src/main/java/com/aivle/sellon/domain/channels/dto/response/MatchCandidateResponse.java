package com.aivle.sellon.domain.channels.dto.response;

import java.util.List;

public record MatchCandidateResponse(
        Long masterProductKey,
        String productGroupId,
        String productName,
        double similarityScore,
        boolean topRecommendation,
        String reason,
        List<String> channels
) {
}

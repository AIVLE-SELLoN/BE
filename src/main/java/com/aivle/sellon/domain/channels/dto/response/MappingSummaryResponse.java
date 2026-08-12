package com.aivle.sellon.domain.channels.dto.response;

public record MappingSummaryResponse(
        long unmatchedCount,
        long matchedCount,
        long totalCount
) {
    public static MappingSummaryResponse of(long unmatchedCount, long matchedCount) {
        return new MappingSummaryResponse(unmatchedCount, matchedCount, unmatchedCount + matchedCount);
    }
}

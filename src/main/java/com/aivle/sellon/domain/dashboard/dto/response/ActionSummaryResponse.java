package com.aivle.sellon.domain.dashboard.dto.response;

import com.aivle.sellon.domain.alert.enums.RecommendedAction;

public record ActionSummaryResponse(
        String action,
        String actionName,
        long count
) {
    public static ActionSummaryResponse of(RecommendedAction action, long count) {
        return new ActionSummaryResponse(action.name(), action.getJsonValue(), count);
    }
}

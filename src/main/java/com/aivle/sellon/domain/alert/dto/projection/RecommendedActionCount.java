package com.aivle.sellon.domain.alert.dto.projection;

import com.aivle.sellon.domain.alert.enums.RecommendedAction;

public interface RecommendedActionCount {

    RecommendedAction getRecommendedAction();

    Long getCount();
}

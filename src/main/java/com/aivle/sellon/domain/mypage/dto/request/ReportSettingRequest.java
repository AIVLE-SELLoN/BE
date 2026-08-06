package com.aivle.sellon.domain.mypage.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.LocalTime;
import java.util.List;

public record ReportSettingRequest(
        boolean enabled,
        @Min(1) @Max(28) int sendDay,
        @NotNull LocalTime sendTime,
        @NotNull @Valid List<RecipientRequest> recipients
) {
}

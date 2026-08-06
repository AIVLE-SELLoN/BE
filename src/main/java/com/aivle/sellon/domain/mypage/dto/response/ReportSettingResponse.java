package com.aivle.sellon.domain.mypage.dto.response;

import com.aivle.sellon.domain.mypage.entity.MonthlyReportSetting;

import java.time.format.DateTimeFormatter;
import java.util.List;

public record ReportSettingResponse(
        boolean enabled,
        int sendDay,
        String sendTime,
        List<RecipientResponse> recipients
) {
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    public static ReportSettingResponse of(MonthlyReportSetting setting, List<RecipientResponse> recipients) {
        return new ReportSettingResponse(
                setting.isEnabled(),
                setting.getSendDay(),
                setting.getSendTime().format(TIME_FORMATTER),
                recipients
        );
    }

    public static ReportSettingResponse defaultValue() {
        return new ReportSettingResponse(false, 1, "10:00", List.of());
    }
}

package com.aivle.sellon.domain.alert.dto.message;

import com.aivle.sellon.domain.alert.enums.AlertChannel;
import com.aivle.sellon.domain.alert.enums.Aspect;
import com.aivle.sellon.domain.alert.enums.DetectionConfidence;
import com.aivle.sellon.domain.alert.enums.RecommendedAction;
import com.aivle.sellon.domain.alert.enums.StatsSource;
import com.aivle.sellon.domain.alert.enums.Verdict;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import tools.jackson.databind.JsonNode;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

@JsonIgnoreProperties({"recommendation", "channel_breakdown_snapshot"})
public record AlertAnalyzedPayload(
        @JsonProperty("alert_id") String alertId,
        @JsonProperty("detected_at") OffsetDateTime detectedAt,
        @JsonProperty("updates_alert_id") String updatesAlertId,
        @JsonProperty("product_group_id") String productGroupId,
        AlertChannel channel,
        @JsonProperty("window_start") LocalDate windowStart,
        @JsonProperty("window_end") LocalDate windowEnd,
        Verdict verdict,
        @JsonProperty("significant_channels") JsonNode significantChannels,
        @JsonProperty("excluded_channels") JsonNode excludedChannels,
        @JsonProperty("main_aspect") Aspect mainAspect,
        @JsonProperty("sub_aspects") JsonNode subAspects,
        Stats stats,
        @JsonProperty("source_signals") JsonNode sourceSignals,
        @JsonProperty("root_cause") RootCause rootCause,
        @JsonProperty("detection_confidence") DetectionConfidence detectionConfidence,
        @JsonProperty("scope_in") Boolean scopeIn,
        @JsonProperty("recommended_action") RecommendedAction recommendedAction,
        Evidence evidence,
        @JsonProperty("channel_rates") List<ChannelRate> channelRates
) {
    public record Stats(
            StatsSource source,
            @JsonProperty("cur_rate") BigDecimal curRate,
            @JsonProperty("past_rate") BigDecimal pastRate,
            BigDecimal delta,
            @JsonProperty("p_value") Double pValue,
            @JsonProperty("bh_significant") Boolean bhSignificant,
            @JsonProperty("cur_total") Integer curTotal
    ) {
    }

    public record RootCause(
            String label,
            Integer count,
            Integer total,
            Boolean consistent
    ) {
    }

    public record ChannelRate(
            String channel,
            BigDecimal rate,
            Boolean excluded,
            Integer total
    ) {
    }

    /**
     * payload는 evidence를 중첩 객체로 보내지만(메시지 큐 컨벤션 4.1절),
     * DetectionAlert는 두 값을 평면 컬럼으로 저장하므로 여기서만 중첩을 받고 서비스에서 펼친다.
     */
    public record Evidence(
            @JsonProperty("inquiry_ids") JsonNode inquiryIds,
            @JsonProperty("linked_change_id") String linkedChangeId
    ) {
    }
}

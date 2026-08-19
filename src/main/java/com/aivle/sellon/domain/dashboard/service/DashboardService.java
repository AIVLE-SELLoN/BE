package com.aivle.sellon.domain.dashboard.service;

import com.aivle.sellon.domain.alert.dto.projection.RecommendedActionCount;
import com.aivle.sellon.domain.alert.enums.AlertChannel;
import com.aivle.sellon.domain.alert.enums.RecommendedAction;
import com.aivle.sellon.domain.alert.repository.DetectionAlertRepository;
import com.aivle.sellon.domain.dashboard.dto.response.ActionSummaryResponse;
import com.aivle.sellon.domain.dashboard.dto.response.ChannelSummaryResponse;
import com.aivle.sellon.domain.dashboard.dto.response.DashboardResponse;
import com.aivle.sellon.domain.notification.repository.NotificationRepository;
import com.aivle.sellon.global.security.principal.UserPrincipal;
import com.aivle.sellon.rawdb.dto.ChannelMetricRow;
import com.aivle.sellon.rawdb.service.RawDashboardMetricReader;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private static final ZoneId DASHBOARD_ZONE_ID = ZoneId.of("Asia/Seoul");
    private static final List<AlertChannel> DASHBOARD_CHANNELS = List.of(
            AlertChannel.COUPANG,
            AlertChannel.NAVER,
            AlertChannel.ZIGZAG
    );

    private final NotificationRepository notificationRepository;
    private final RawDashboardMetricReader rawDashboardMetricReader;
    private final DetectionAlertRepository detectionAlertRepository;

    @Transactional(readOnly = true)
    public DashboardResponse getDashboard(UserPrincipal principal, String period) {
        int periodDays = toPeriodDays(period);
        LocalDate today = LocalDate.now(DASHBOARD_ZONE_ID);
        LocalDate fromInclusive = today.minusDays(periodDays - 1L);
        LocalDate toExclusive = today.plusDays(1);
        OffsetDateTime timestampFromInclusive = fromInclusive.atStartOfDay(DASHBOARD_ZONE_ID).toOffsetDateTime();
        OffsetDateTime timestampToExclusive = toExclusive.atStartOfDay(DASHBOARD_ZONE_ID).toOffsetDateTime();

        RawDashboardMetricReader.RawDashboardMetrics metrics = rawDashboardMetricReader.read(
                timestampFromInclusive, timestampToExclusive, fromInclusive, toExclusive);

        Map<String, Long> csCounts = metrics.csCounts()
                .stream()
                .collect(Collectors.toMap(ChannelMetricRow.CsCount::getChannelId,
                        ChannelMetricRow.CsCount::getCount));

        Map<String, Long> orderCounts = metrics.orderQuantities()
                .stream()
                .collect(Collectors.toMap(ChannelMetricRow.OrderQuantity::getChannelId,
                        ChannelMetricRow.OrderQuantity::getTotalQuantity));

        Map<String, Double> averageRatings = new HashMap<>();
        metrics.reviewRatings()
                .forEach(row -> averageRatings.put(row.getChannelId(), row.getAvgRating()));

        List<ChannelSummaryResponse> channelSummary = DASHBOARD_CHANNELS.stream()
                .map(channel -> ChannelSummaryResponse.of(
                        channel,
                        toChannelName(channel),
                        csCounts.getOrDefault(channel.name(), 0L),
                        orderCounts.getOrDefault(channel.name(), 0L),
                        roundAverageRating(averageRatings.get(channel.name()))
                ))
                .toList();

        long unreadNotificationCount = notificationRepository.countUnreadByCompanyId(principal.getCompanyId());
        List<ActionSummaryResponse> actionSummary = buildActionSummary(principal.getCompanyId());
        return DashboardResponse.of(unreadNotificationCount, channelSummary, actionSummary);
    }

    private List<ActionSummaryResponse> buildActionSummary(Long companyId) {
        Map<RecommendedAction, Long> counts = detectionAlertRepository.countByRecommendedAction(companyId)
                .stream()
                .collect(Collectors.toMap(RecommendedActionCount::getRecommendedAction, RecommendedActionCount::getCount));

        return Arrays.stream(RecommendedAction.values())
                .map(action -> ActionSummaryResponse.of(action, counts.getOrDefault(action, 0L)))
                .sorted(Comparator.comparingLong(ActionSummaryResponse::count).reversed())
                .toList();
    }

    private int toPeriodDays(String period) {
        // 허용값 검증은 컨트롤러의 @Pattern이 담당한다. 여기서는 매핑만 한다.
        return switch (period) {
            case "1D" -> 1;
            case "30D" -> 30;
            default -> 7;
        };
    }

    private String toChannelName(AlertChannel channel) {
        return switch (channel) {
            case COUPANG -> "쿠팡";
            case NAVER -> "네이버";
            case ZIGZAG -> "지그재그";
            case ALL -> "전체";
        };
    }

    private Double roundAverageRating(Double averageRating) {
        if (averageRating == null) {
            return null;
        }
        return BigDecimal.valueOf(averageRating)
                .setScale(1, RoundingMode.HALF_UP)
                .doubleValue();
    }
}

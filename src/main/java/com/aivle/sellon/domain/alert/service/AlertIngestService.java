package com.aivle.sellon.domain.alert.service;

import com.aivle.sellon.domain.alert.dto.message.AlertAnalyzedPayload;
import com.aivle.sellon.domain.alert.entity.AlertRootCause;
import com.aivle.sellon.domain.alert.entity.AlertStats;
import com.aivle.sellon.domain.alert.entity.DetectionAlert;
import com.aivle.sellon.domain.alert.enums.AlertChannel;
import com.aivle.sellon.domain.alert.enums.StatsSource;
import com.aivle.sellon.domain.alert.repository.DetectionAlertRepository;
import com.aivle.sellon.domain.company.entity.Company;
import com.aivle.sellon.domain.company.exception.CompanyNotFoundException;
import com.aivle.sellon.domain.company.repository.CompanyRepository;
import com.aivle.sellon.domain.notification.entity.Notification;
import com.aivle.sellon.domain.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;

@Service
@RequiredArgsConstructor
public class AlertIngestService {

    private static final ZoneId KOREA_ZONE_ID = ZoneId.of("Asia/Seoul");

    private final DetectionAlertRepository detectionAlertRepository;
    private final NotificationRepository notificationRepository;
    private final CompanyRepository companyRepository;
    private final JsonMapper jsonMapper;

    @Transactional
    public void ingest(AlertAnalyzedPayload payload, String companyKey) {
        Company company = findCompany(companyKey);

        DetectionAlert existing = detectionAlertRepository.findByAlertCode(payload.alertId()).orElse(null);
        if (existing != null) {
            update(existing, payload);
            return;
        }

        DetectionAlert alert = detectionAlertRepository.save(create(payload, company));
        notificationRepository.save(Notification.createForAlert(
                company, buildNotificationMessage(payload), toKoreaStandardTime(payload.detectedAt()), alert.getId()));
    }

    private Company findCompany(String companyKey) {
        if (companyKey == null || companyKey.isBlank()) {
            throw new CompanyNotFoundException();
        }

        return companyRepository.findByJoinKey(companyKey)
                .orElseThrow(CompanyNotFoundException::new);
    }

    private DetectionAlert create(AlertAnalyzedPayload payload, Company company) {
        return DetectionAlert.create(
                payload.alertId(), toKoreaStandardTime(payload.detectedAt()), payload.updatesAlertId(), company,
                payload.productGroupId(), payload.channel(), payload.windowStart(), payload.windowEnd(),
                payload.verdict(), toJson(payload.significantChannels()), toJson(payload.excludedChannels()),
                payload.mainAspect(), toJson(payload.subAspects()), toStats(payload.stats()),
                toJson(payload.sourceSignals()), toRootCause(payload.rootCause()), payload.detectionConfidence(),
                payload.scopeIn(), payload.recommendedAction(), toJson(payload.evidence().inquiryIds()),
                payload.evidence().linkedChangeId(), toJsonString(payload.channelRates())
        );
    }

    private void update(DetectionAlert alert, AlertAnalyzedPayload payload) {
        alert.update(
                toKoreaStandardTime(payload.detectedAt()), payload.updatesAlertId(), payload.productGroupId(), payload.channel(),
                payload.windowStart(), payload.windowEnd(), payload.verdict(), toJson(payload.significantChannels()),
                toJson(payload.excludedChannels()), payload.mainAspect(), toJson(payload.subAspects()),
                toStats(payload.stats()), toJson(payload.sourceSignals()), toRootCause(payload.rootCause()),
                payload.detectionConfidence(), payload.scopeIn(), payload.recommendedAction(),
                toJson(payload.evidence().inquiryIds()), payload.evidence().linkedChangeId(), toJsonString(payload.channelRates())
        );
    }

    private AlertStats toStats(AlertAnalyzedPayload.Stats stats) {
        return AlertStats.create(stats.source(), stats.curRate(), stats.pastRate(), stats.delta(), stats.pValue(),
                stats.bhSignificant(), stats.curTotal());
    }

    private AlertRootCause toRootCause(AlertAnalyzedPayload.RootCause rootCause) {
        return rootCause == null ? null
                : AlertRootCause.create(rootCause.label(), rootCause.count(), rootCause.total(), rootCause.consistent());
    }

    private String buildNotificationMessage(AlertAnalyzedPayload payload) {
        String channel = toChannelLabel(payload.channel());
        String phrase = toSourcePhrase(payload.stats().source());
        return "%s · %s · %s %s (%s%% → %s%%)".formatted(
                payload.productGroupId(), channel, payload.mainAspect().getJsonValue(), phrase,
                toPercent(payload.stats().pastRate()), toPercent(payload.stats().curRate()));
    }

    // switch expression으로 두면 StatsSource에 값이 추가될 때 컴파일 단계에서 누락이 잡힌다.
    private String toSourcePhrase(StatsSource source) {
        return switch (source) {
            case CS -> "문의 급증";
            case REVIEW -> "리뷰 부정 급증";
        };
    }

    // AlertChannel은 payload에 영문 값으로 실려오므로(메시지 큐 컨벤션 6절) 표시용 한글 라벨은 여기서 붙인다.
    private String toChannelLabel(AlertChannel channel) {
        return switch (channel) {
            case COUPANG -> "쿠팡";
            case NAVER -> "네이버";
            case ZIGZAG -> "지그재그";
            case ALL -> "전 채널";
        };
    }

    private String toPercent(BigDecimal rate) {
        return rate.movePointRight(2).setScale(1, RoundingMode.HALF_UP).toPlainString();
    }

    private String toJson(JsonNode node) {
        return node == null || node.isNull() ? null : node.toString();
    }

    private String toJsonString(Object value) {
        return value == null ? null : jsonMapper.writeValueAsString(value);
    }

    private LocalDateTime toKoreaStandardTime(OffsetDateTime detectedAt) {
        return detectedAt.atZoneSameInstant(KOREA_ZONE_ID).toLocalDateTime();
    }
}

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

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
@RequiredArgsConstructor
public class AlertIngestService {

    private final DetectionAlertRepository detectionAlertRepository;
    private final NotificationRepository notificationRepository;
    private final CompanyRepository companyRepository;

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
                company, buildNotificationMessage(payload), payload.detectedAt(), alert.getId()));
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
                payload.alertId(), payload.detectedAt(), payload.updatesAlertId(), company,
                payload.productGroupId(), payload.channel(), payload.windowStart(), payload.windowEnd(),
                payload.verdict(), toJson(payload.significantChannels()), toJson(payload.excludedChannels()),
                payload.mainAspect(), toJson(payload.subAspects()), toStats(payload.stats()),
                toJson(payload.sourceSignals()), toRootCause(payload.rootCause()), payload.detectionConfidence(),
                payload.scopeIn(), payload.recommendedAction(), toJson(payload.evidenceInquiryIds()),
                payload.linkedChangeId(), null
        );
    }

    private void update(DetectionAlert alert, AlertAnalyzedPayload payload) {
        alert.update(
                payload.detectedAt(), payload.updatesAlertId(), payload.productGroupId(), payload.channel(),
                payload.windowStart(), payload.windowEnd(), payload.verdict(), toJson(payload.significantChannels()),
                toJson(payload.excludedChannels()), payload.mainAspect(), toJson(payload.subAspects()),
                toStats(payload.stats()), toJson(payload.sourceSignals()), toRootCause(payload.rootCause()),
                payload.detectionConfidence(), payload.scopeIn(), payload.recommendedAction(),
                toJson(payload.evidenceInquiryIds()), payload.linkedChangeId(), null
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
        String channel = payload.channel() == AlertChannel.ALL ? "전 채널" : payload.channel().getJsonValue();
        String phrase = payload.stats().source() == StatsSource.CS ? "문의 급증" : "리뷰 부정 급증";
        return "%s · %s · %s %s (%s%% → %s%%)".formatted(
                payload.productGroupId(), channel, payload.mainAspect().getJsonValue(), phrase,
                toPercent(payload.stats().pastRate()), toPercent(payload.stats().curRate()));
    }

    private String toPercent(BigDecimal rate) {
        return rate.movePointRight(2).setScale(1, RoundingMode.HALF_UP).toPlainString();
    }

    private String toJson(JsonNode node) {
        return node == null || node.isNull() ? null : node.toString();
    }
}

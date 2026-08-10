package com.aivle.sellon.domain.proposal.consumer;

import com.aivle.sellon.domain.company.entity.Company;
import com.aivle.sellon.domain.company.exception.CompanyNotFoundException;
import com.aivle.sellon.domain.company.repository.CompanyRepository;
import com.aivle.sellon.domain.proposal.dto.message.AlertAnalyzedPayload;
import com.aivle.sellon.domain.proposal.enums.RecommendedAction;
import com.aivle.sellon.domain.proposal.service.ProposalIngestService;
import com.aivle.sellon.domain.user.entity.User;
import com.aivle.sellon.domain.user.exception.UserNotFoundException;
import com.aivle.sellon.domain.user.repository.UserRepository;
import com.aivle.sellon.global.mq.enums.EventFailureReason;
import com.aivle.sellon.global.mq.listener.MqEventHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@Slf4j
@Component
@RequiredArgsConstructor
public class AlertDetectedHandler implements MqEventHandler {

    private static final String ALERT_ANALYZED_EVENT_TYPE = "ai.anomaly.analyzed";

    private final ProposalIngestService proposalIngestService;
    private final CompanyRepository companyRepository;
    private final UserRepository userRepository;
    private final JsonMapper jsonMapper;

    @Override
    public boolean supports(String eventType) {
        return ALERT_ANALYZED_EVENT_TYPE.equals(eventType);
    }

    @Override
    public void handle(JsonNode envelope, String eventId, String traceId) {
        // company_id는 payload 스키마에 없다. envelope 최상위에 회사 식별키(join_key)로 실려 온다.
        String companyKey = envelope.path("companyId").asString(null);

        // 파싱/회사·유저 조회를 한 try에 두면 서비스 내부 예외가 MALFORMED_PAYLOAD로
        // 오분류돼 재시도 없이 DLQ로 직행한다. 범위를 나눠 둔다.
        AlertAnalyzedPayload payload;
        try {
            payload = readPayload(envelope);
        } catch (JacksonException | IllegalArgumentException e) {
            throw deadLetter(EventFailureReason.MALFORMED_PAYLOAD, eventId, traceId, e);
        }

        // recommended_action이 "개선안 생성"이 아닌 6종 조치는 recommendation이 항상 null이라
        // 개선안 리포트를 만들 근거가 없다. 알림 자체는 이 도메인 소관이 아니라 여기서는 조용히 건너뛴다.
        RecommendedAction recommendedAction = RecommendedAction.fromKorean(payload.recommendedAction());
        if (recommendedAction != RecommendedAction.GENERATE_RECOMMENDATION || payload.recommendation() == null) {
            log.info("개선안 생성 대상 아님 - alertId={}, recommendedAction={}", payload.alertId(), recommendedAction);
            return;
        }

        User rootUser;
        try {
            rootUser = findRootUser(companyKey);
        } catch (CompanyNotFoundException | UserNotFoundException e) {
            throw deadLetter(EventFailureReason.UNKNOWN_COMPANY, eventId, traceId, e);
        }

        proposalIngestService.ingestAnalyzedAlert(rootUser, payload);
    }

    private AlertAnalyzedPayload readPayload(JsonNode envelope) {
        JsonNode payloadNode = envelope.get("payload");
        if (payloadNode == null || payloadNode.isNull())
            throw new IllegalArgumentException("payload 없음");

        AlertAnalyzedPayload payload = jsonMapper.treeToValue(payloadNode, AlertAnalyzedPayload.class);
        validate(payload);
        return payload;
    }

    /**
     * 필수 필드가 비면 저장 단계에서 제약 위반으로 터지는데, 그러면 일시적 장애와 구분되지 않아
     * 5회 재시도를 낭비한다. 여기서 걸러 곧장 DLQ로 보낸다.
     */
    private void validate(AlertAnalyzedPayload payload) {
        if (payload.alertId() == null || payload.alertId().isBlank())
            throw new IllegalArgumentException("alert_id 없음");

        if (payload.detectedAt() == null)
            throw new IllegalArgumentException("detected_at 없음");

        if (payload.recommendedAction() == null || payload.recommendedAction().isBlank())
            throw new IllegalArgumentException("recommended_action 없음");
    }

    // company_id는 DB PK가 아니라 회원가입 시 발급되는 회사 식별키(join_key)다. (ReportService.findCompany와 동일 컨벤션)
    private User findRootUser(String companyKey) {
        if (companyKey == null || companyKey.isBlank())
            throw new CompanyNotFoundException();

        Company company = companyRepository.findByJoinKey(companyKey)
                .orElseThrow(CompanyNotFoundException::new);

        // 회사의 ROOT 계정을 rootUser로 사용 (Proposal.rootUser는 회사 스코핑용 FK일 뿐, ROOT 전용 제약이 아님)
        return userRepository.findRootByCompanyIdAndDeletedAtIsNull(company.getId())
                .orElseThrow(UserNotFoundException::new);
    }

    /**
     * 재시도가 무의미한 실패는 requeue 없이 곧장 DLX로 보낸다.
     * 브로커의 x-death 헤더는 rejected/expired만 구분하므로 사유는 로그로 남긴다.
     */
    private AmqpRejectAndDontRequeueException deadLetter(
            EventFailureReason reason, String eventId, String traceId, Exception cause
    ) {
        log.error("이벤트 DLQ 이동 - reason={}, detail={}, eventType={}, eventId={}, traceId={}",
                reason, reason.getDescription(), ALERT_ANALYZED_EVENT_TYPE, eventId, traceId, cause);

        return new AmqpRejectAndDontRequeueException(
                "%s (eventId=%s)".formatted(reason, eventId), cause);
    }
}

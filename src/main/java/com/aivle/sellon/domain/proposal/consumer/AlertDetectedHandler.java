package com.aivle.sellon.domain.proposal.consumer;

import com.aivle.sellon.domain.proposal.event.AlertDetectedEvent;
import com.aivle.sellon.domain.proposal.service.ProposalService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

// 탐지 알림 컨슈머 진입점.
// TODO: 실제 브로커 리스너(@KafkaListener 등) 연결 — 토픽 이름 확정 후 반영
@Component
@RequiredArgsConstructor
public class AlertDetectedHandler {

    private final ProposalService proposalService;

    public void onAlertDetected(AlertDetectedEvent event) {
        // TODO: event.companyId()는 확보됨. 이 회사의 ROOT 계정을 찾아 rootUser로 넘겨야 하는데,
        // "회사 id로 유저 조회"
        // user 도메인에 조회 메서드(예: findByCompany_IdAndRole)가 추가되면 여기서 사용해 rootUser를 채울 것.
        proposalService.generateAndUpsertProposal(null, event);
    }
}

package com.aivle.sellon.domain.proposal.event;

/**
 * 개선안 승인 시 발행. DetectionAlert.alertStatus를 RESOLVED로 전환하기 위한 트리거.
 * 리스너가 트랜잭션 밖에서 처리하므로 지연 로딩되는 엔티티 대신 식별자만 담는다.
 * <p>
 * alertCode는 Proposal.alertId(String, = DetectionAlert.alertCode)를 그대로 옮긴 것이다.
 * DetectionAlert 조회는 (companyId, alertCode) 복합키로 한다 — alert_code 단독은
 * 회사 간 충돌 가능성이 있어 유니크가 아니다.
 */
public record ProposalAcceptedEvent(
        Long companyId,
        String alertCode
) {
}

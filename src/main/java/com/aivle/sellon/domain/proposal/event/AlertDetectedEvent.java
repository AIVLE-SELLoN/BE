package com.aivle.sellon.domain.proposal.event;

import java.time.LocalDateTime;

// alertType 등 세부 필드는 탐지 배치 스케줄러/이상탐지 이벤트 스펙 확정 후 보강 필요.
public record AlertDetectedEvent(
    String alertId,
    String alertType, // Type A / Type B
    Long companyId,
    LocalDateTime detectedAt
) {}

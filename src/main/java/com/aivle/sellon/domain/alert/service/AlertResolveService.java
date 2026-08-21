package com.aivle.sellon.domain.alert.service;

import com.aivle.sellon.domain.alert.entity.DetectionAlert;
import com.aivle.sellon.domain.alert.repository.DetectionAlertRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 개선안 도메인(모듈 경계를 넘는) 이벤트로부터 DetectionAlert 상태를 전환하는 전담 서비스.
 * <p>
 * REQUIRES_NEW인 이유: AFTER_COMMIT 리스너에서 호출된다. 그 시점엔 원본(개선안 승인)
 * 트랜잭션이 이미 끝나 있어서, 기본 전파(REQUIRED)로 두면 죽은 트랜잭션에 조인해
 * save가 커밋 없이 조용히 사라진다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AlertResolveService {

    private final DetectionAlertRepository detectionAlertRepository;

    /**
     * alertCode에 매칭되는 DetectionAlert가 없어도 예외를 던지지 않는다. 개선안이 알림보다
     * 먼저 존재할 이유는 없지만(같은 ai.anomaly.analyzed에서 함께 나옴), 데이터 정합성이
     * 어긋난 경우까지 승인 처리 자체를 막을 이유는 없다 — 로그로만 남긴다.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void resolve(Long companyId, String alertCode) {
        detectionAlertRepository.findByCompanyIdAndAlertCode(companyId, alertCode)
                .ifPresentOrElse(
                        DetectionAlert::resolve,
                        () -> log.warn("해결 처리 대상 DetectionAlert를 찾을 수 없습니다. companyId={}, alertCode={}",
                                companyId, alertCode)
                );
    }
}

package com.aivle.sellon.domain.guideline.repository;

import com.aivle.sellon.domain.guideline.entity.Guideline;

import java.util.List;
import java.util.Optional;

public interface GuidelineRepositoryCustom {

    /**
     * 큐 재전달 시 이미 저장된 가이드라인인지 판단하는 upsert 조회용.
     * guidelineId는 alert_id와 1:1이지만 회사 구분자가 없어 단독으로는 유일하지 않다.
     */
    Optional<Guideline> findByCompanyIdAndGuidelineId(Long companyId, String guidelineId);

    /**
     * 파일 히스토리 조회. 커서(guideline_id) 기준 최신순이며,
     * limit은 hasNext 판단을 위해 호출부가 요청 size + 1로 넘긴다.
     * 한 번도 파일이 올라온 적 없는 건(생성 실패 등)은 내려받을 대상이 아니라 제외한다.
     */
    List<Guideline> findAllWithFileByCompanyId(Long companyId, Long cursorId, int limit);

    /**
     * 재생성 대상을 락과 함께 가져온다. 같은 파일에 대한 다운로드 요청이 동시에 들어와도
     * PDF를 두 번 만들어 올리지 않도록, 앞선 요청이 끝날 때까지 기다렸다 최신 상태를 다시 읽는다.
     */
    Optional<Guideline> findByIdForUpdate(Long id);
}

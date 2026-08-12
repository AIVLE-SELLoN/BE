package com.aivle.sellon.rawdb.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * input_detail_changes.csv 스키마 확정(2026-08-11) 반영. 「Raw DB 스키마 확정 (8/7)」 §1
 * 테이블 목록에는 아직 상세페이지 변경용 테이블이 없어(공식 확정 전) 우리 쪽에서 자체적으로
 * 구조화해 둔 테이블이다 - 나중에 정식 확정 스키마가 나오면 컬럼명을 맞춰 조정할 수 있다.
 * 이상탐지(Agent2)가 원인 근거(evidence.linked_change_id)로 참조할 데이터다.
 *
 * change_type은 aspect 6종(색상/사이즈/소재/파손/오배송/기타) + "initial"(최초 상세설명 등록)이
 * 섞여 있어 InquiryType enum으로 강제하지 않고 원문 문자열 그대로 둔다.
 */
@Entity
@Table(name = "detail_change")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RawDetailChange {

    @Id
    @Column(name = "change_id")
    private String changeId;

    @Column(name = "channel_id")
    private String channelId;

    @Column(name = "channel_product_id")
    private String channelProductId;

    @Column(name = "changed_field")
    private String changedField;

    @Column(name = "previous_value")
    private String previousValue;

    @Column(name = "new_value")
    private String newValue;

    /**
     * aspect 6종 한글 라벨 또는 "initial". 최초 상세설명 등록 행은 previous_value가 NULL.
     */
    @Column(name = "change_type")
    private String changeType;

    @Column(name = "changed_at")
    private LocalDateTime changedAt;

    public static RawDetailChange of(String changeId, String channelId, String channelProductId,
                                      String changedField, String previousValue, String newValue,
                                      String changeType, LocalDateTime changedAt) {
        RawDetailChange entity = new RawDetailChange();
        entity.changeId = changeId;
        entity.channelId = channelId;
        entity.channelProductId = channelProductId;
        entity.changedField = changedField;
        entity.previousValue = previousValue;
        entity.newValue = newValue;
        entity.changeType = changeType;
        entity.changedAt = changedAt;
        return entity;
    }
}

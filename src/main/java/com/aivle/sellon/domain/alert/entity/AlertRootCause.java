package com.aivle.sellon.domain.alert.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AlertRootCause {

    // null은 원인분류 미수행을 뜻하고, '미특정'은 수행했으나 원인이 분산된 상태를 뜻하므로 서로 구분한다.
    @Column(length = 50)
    private String label;

    private Integer count;
    private Integer total;
    private Boolean consistent;

    private AlertRootCause(String label, Integer count, Integer total, Boolean consistent) {
        this.label = label;
        this.count = count;
        this.total = total;
        this.consistent = consistent;
    }

    public static AlertRootCause create(String label, Integer count, Integer total, Boolean consistent) {
        return new AlertRootCause(label, count, total, consistent);
    }

    public void update(String label, Integer count, Integer total, Boolean consistent) {
        this.label = label;
        this.count = count;
        this.total = total;
        this.consistent = consistent;
    }
}

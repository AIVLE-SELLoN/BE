package com.aivle.sellon.rawdb.entity;

import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDate;

/**
 * 「Raw DB 스키마 확정 (8/7)」§2-9 orders 복합 PK. 개별 주문건이 아니라 그 날의 합산이라
 * (channel_id, channel_product_id, order_date) 조합으로 하루 한 행이다.
 */
@NoArgsConstructor
@EqualsAndHashCode
public class RawOrderId implements Serializable {
    private String channelId;
    private String channelProductId;
    private LocalDate orderDate;

    public RawOrderId(String channelId, String channelProductId, LocalDate orderDate) {
        this.channelId = channelId;
        this.channelProductId = channelProductId;
        this.orderDate = orderDate;
    }
}

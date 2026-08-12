package com.aivle.sellon.rawdb.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 「Raw DB 스키마 확정 (8/7)」§2-9 orders. main server(우리) 소유·쓰기 대상.
 * 채널별 하루 합산 주문 원본 - 개별 주문건이 아니다.
 */
@Entity
@Table(name = "orders")
@IdClass(RawOrderId.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RawOrder {

    @Id
    @Column(name = "channel_id")
    private String channelId;

    @Id
    @Column(name = "channel_product_id")
    private String channelProductId;

    @Id
    @Column(name = "order_date")
    private LocalDate orderDate;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @Column(name = "order_amount", nullable = false)
    private Integer orderAmount;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    public static RawOrder of(String channelId, String channelProductId, LocalDate orderDate,
                               Integer quantity, Integer orderAmount, LocalDateTime createdAt) {
        RawOrder entity = new RawOrder();
        entity.channelId = channelId;
        entity.channelProductId = channelProductId;
        entity.orderDate = orderDate;
        entity.quantity = quantity;
        entity.orderAmount = orderAmount;
        entity.createdAt = createdAt;
        return entity;
    }
}

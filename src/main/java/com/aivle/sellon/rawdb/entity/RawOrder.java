package com.aivle.sellon.rawdb.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.OffsetDateTime;

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
    private Long orderAmount;

    @Column(name = "created_at")
    private OffsetDateTime createdAt;

    public static RawOrder of(String channelId, String channelProductId, LocalDate orderDate,
                               Integer quantity, Long orderAmount, OffsetDateTime createdAt) {
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

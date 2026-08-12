package com.aivle.sellon.rawdb.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Immutable;

import java.time.LocalDate;
import java.time.OffsetDateTime;

@Entity
@Immutable
@Getter
@Table(name = "orders")
@IdClass(RawOrderId.class)
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

    @Column(name = "quantity")
    private Integer quantity;

    @Column(name = "order_amount")
    private Long orderAmount;

    @Column(name = "created_at")
    private OffsetDateTime createdAt;
}

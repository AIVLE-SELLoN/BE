package com.aivle.sellon.rawdb.entity;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;
import java.util.Objects;

@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RawOrderId implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private String channelId;
    private String channelProductId;
    private LocalDate orderDate;

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (!(object instanceof RawOrderId that)) {
            return false;
        }
        return Objects.equals(channelId, that.channelId)
                && Objects.equals(channelProductId, that.channelProductId)
                && Objects.equals(orderDate, that.orderDate);
    }

    @Override
    public int hashCode() {
        return Objects.hash(channelId, channelProductId, orderDate);
    }
}

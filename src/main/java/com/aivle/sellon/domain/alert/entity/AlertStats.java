package com.aivle.sellon.domain.alert.entity;

import com.aivle.sellon.domain.alert.enums.StatsSource;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AlertStats {

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatsSource source;

    @Column(nullable = false, precision = 5, scale = 4)
    private BigDecimal curRate;

    @Column(nullable = false, precision = 5, scale = 4)
    private BigDecimal pastRate;

    @Column(nullable = false, precision = 5, scale = 4)
    private BigDecimal delta;

    private Double pValue;

    @Column(nullable = false)
    private boolean bhSignificant;

    @Column(nullable = false)
    private int curTotal;

    private AlertStats(StatsSource source, BigDecimal curRate, BigDecimal pastRate, BigDecimal delta, Double pValue,
                       boolean bhSignificant, int curTotal) {
        this.source = source;
        this.curRate = curRate;
        this.pastRate = pastRate;
        this.delta = delta;
        this.pValue = pValue;
        this.bhSignificant = bhSignificant;
        this.curTotal = curTotal;
    }

    public static AlertStats create(StatsSource source, BigDecimal curRate, BigDecimal pastRate, BigDecimal delta,
                                    Double pValue, boolean bhSignificant, int curTotal) {
        return new AlertStats(source, curRate, pastRate, delta, pValue, bhSignificant, curTotal);
    }

    public void update(StatsSource source, BigDecimal curRate, BigDecimal pastRate, BigDecimal delta, Double pValue,
                       boolean bhSignificant, int curTotal) {
        this.source = source;
        this.curRate = curRate;
        this.pastRate = pastRate;
        this.delta = delta;
        this.pValue = pValue;
        this.bhSignificant = bhSignificant;
        this.curTotal = curTotal;
    }
}

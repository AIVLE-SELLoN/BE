package com.aivle.sellon.domain.mypage.entity;

import com.aivle.sellon.domain.company.entity.Company;
import com.aivle.sellon.global.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MonthlyReportSetting extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false, unique = true)
    private Company company;

    @Column(nullable = false)
    private boolean enabled;

    @Column(nullable = false)
    private int sendDay;

    @Column(nullable = false)
    private LocalTime sendTime;

    private MonthlyReportSetting(Company company, boolean enabled, int sendDay, LocalTime sendTime) {
        this.company = company;
        this.enabled = enabled;
        this.sendDay = sendDay;
        this.sendTime = sendTime;
    }

    public static MonthlyReportSetting create(Company company, boolean enabled, int sendDay, LocalTime sendTime) {
        return new MonthlyReportSetting(company, enabled, sendDay, sendTime);
    }

    public void update(boolean enabled, int sendDay, LocalTime sendTime) {
        this.enabled = enabled;
        this.sendDay = sendDay;
        this.sendTime = sendTime;
    }
}

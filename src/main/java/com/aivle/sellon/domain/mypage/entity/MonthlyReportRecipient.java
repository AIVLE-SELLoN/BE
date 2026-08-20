package com.aivle.sellon.domain.mypage.entity;

import com.aivle.sellon.domain.company.entity.Company;
import com.aivle.sellon.domain.mypage.enums.RecipientDepartment;
import com.aivle.sellon.global.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MonthlyReportRecipient extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RecipientDepartment department;

    @Column(nullable = false, length = 255)
    private String email;

    private MonthlyReportRecipient(Company company, RecipientDepartment department, String email) {
        this.company = company;
        this.department = department;
        this.email = email;
    }

    public static MonthlyReportRecipient create(Company company, RecipientDepartment department, String email) {
        return new MonthlyReportRecipient(company, department, email);
    }

    public void update(RecipientDepartment department, String email) {
        this.department = department;
        this.email = email;
    }
}

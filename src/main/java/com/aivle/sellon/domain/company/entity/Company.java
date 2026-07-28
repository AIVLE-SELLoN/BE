package com.aivle.sellon.domain.company.entity;

import com.aivle.sellon.global.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Company extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String joinKey;

    @Column(nullable = false)
    private String name;

    private Company(String joinKey, String name) {
        this.joinKey = joinKey;
        this.name = name;
    }

    public static Company create(String joinKey, String name) {
        return new Company(joinKey, name);
    }
}

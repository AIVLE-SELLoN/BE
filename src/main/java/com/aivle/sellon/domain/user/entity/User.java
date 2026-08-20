package com.aivle.sellon.domain.user.entity;

import com.aivle.sellon.domain.company.entity.Company;
import com.aivle.sellon.domain.user.enums.Role;
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
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @Column(length = 512)
    private String profileImageKey;

    private User(String email, String password, String name, Role role, Company company) {
        this.email = email;
        this.password = password;
        this.name = name;
        this.role = role;
        this.company = company;
    }

    public static User createRoot(String email, String password, String name, Company company) {
        return new User(email, password, name, Role.ROOT, company);
    }

    public static User createMember(String email, String password, String name, Company company) {
        return new User(email, password, name, Role.MEMBER, company);
    }

    public static User createAdmin(String email, String password, String name, Company company) {
        return new User(email, password, name, Role.ADMIN, company);
    }

    public void withdraw() {
        delete();
    }

    public void changeEmail(String email) {
        this.email = email;
    }

    public void changeProfileImage(String profileImageKey) {
        this.profileImageKey = profileImageKey;
    }

    public void removeProfileImage() {
        this.profileImageKey = null;
    }
}

package com.aivle.sellon.global.security.principal;

import com.aivle.sellon.domain.user.entity.User;
import com.aivle.sellon.domain.user.enums.Role;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.List;

@Getter
public class UserPrincipal implements UserDetails {

    private final Long id;
    private final String email;
    private final String password;
    private final Role role;
    private final Long companyId;
    private final boolean enabled;

    private UserPrincipal(Long id, String email, String password, Role role, Long companyId, boolean enabled) {
        this.id = id;
        this.email = email;
        this.password = password;
        this.role = role;
        this.companyId = companyId;
        this.enabled = enabled;
    }

    public static UserPrincipal of(User user) {
        return new UserPrincipal(
                user.getId(),
                user.getEmail(),
                user.getPassword(),
                user.getRole(),
                user.getCompany().getId(),
                user.getDeletedAt() == null
        );
    }

    public static UserPrincipal ofClaims(Long id, String email, Role role, Long companyId) {
        return new UserPrincipal(id, email, null, role, companyId, true);
    }

    @Override
    public List<GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return enabled;
    }

    @Override
    public boolean isAccountNonLocked() {
        return enabled;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return enabled;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }
}

package com.aivle.sellon.global.security.service;

import com.aivle.sellon.domain.user.repository.UserRepository;
import com.aivle.sellon.global.security.principal.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String email) {
        return userRepository.findByEmailAndDeletedAtIsNull(email)
                .map(UserPrincipal::of)
                .orElseThrow(() -> new UsernameNotFoundException("존재하지 않는 이메일입니다."));
    }
}

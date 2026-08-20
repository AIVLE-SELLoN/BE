package com.aivle.sellon.domain.user.service;

import com.aivle.sellon.domain.user.dto.response.UserResponse;
import com.aivle.sellon.domain.user.entity.User;
import com.aivle.sellon.domain.user.exception.UserNotFoundException;
import com.aivle.sellon.domain.user.repository.UserRepository;
import com.aivle.sellon.global.redis.service.RefreshTokenRedisService;
import com.aivle.sellon.global.redis.service.TokenBlacklistRedisService;
import com.aivle.sellon.global.security.principal.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final RefreshTokenRedisService refreshTokenRedisService;
    private final TokenBlacklistRedisService tokenBlacklistRedisService;

    public UserResponse getMe(UserPrincipal principal) {
        User user = findActiveUser(principal.getId());
        return UserResponse.of(user);
    }

    @Transactional
    public void withdraw(UserPrincipal principal, String accessToken) {
        User user = findActiveUser(principal.getId());
        user.withdraw();

        tokenBlacklistRedisService.blacklist(accessToken);
        refreshTokenRedisService.deleteByUserId(user.getId());
    }

    private User findActiveUser(Long id) {
        return userRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(UserNotFoundException::new);
    }
}

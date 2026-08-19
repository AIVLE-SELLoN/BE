package com.aivle.sellon.domain.channels.service.connection;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChannelKeyValidatorTest {

    private final ChannelKeyValidator validator = new ChannelKeyValidator();

    @Test
    @DisplayName("channelType 이 null 이면 NPE 없이 false 를 반환한다")
    void validateFormat_nullChannelType_returnsFalseWithoutException() {
        assertFalse(validator.validateFormat(null, "cp_live_abcdefghijkl"));
    }

    @Test
    @DisplayName("channelCode 가 null 이면 false 를 반환한다")
    void validateFormat_nullChannelCode_returnsFalse() {
        assertFalse(validator.validateFormat("COUPANG", null));
    }

    @Test
    @DisplayName("channelType, channelCode 둘 다 null 이어도 NPE 없이 false 를 반환한다")
    void validateFormat_bothNull_returnsFalseWithoutException() {
        assertFalse(validator.validateFormat(null, null));
    }

    @Test
    @DisplayName("쿠팡 키 형식이 올바르면 true 를 반환한다")
    void validateFormat_validCoupangKey_returnsTrue() {
        assertTrue(validator.validateFormat("COUPANG", "cp_live_abcdefghijkl"));
    }

    @Test
    @DisplayName("지그재그 키 형식이 올바르면 true 를 반환한다")
    void validateFormat_validZigzagKey_returnsTrue() {
        assertTrue(validator.validateFormat("ZIGZAG", "zg_live_abcdefghijkl"));
    }

    @Test
    @DisplayName("지원하지 않는 channelType 이면 false 를 반환한다")
    void validateFormat_unsupportedChannelType_returnsFalse() {
        assertFalse(validator.validateFormat("NAVER", "any-code"));
    }

    @Test
    @DisplayName("접두사가 다른 채널의 키 형식이면 false 를 반환한다")
    void validateFormat_wrongPrefix_returnsFalse() {
        assertFalse(validator.validateFormat("COUPANG", "zg_live_abcdefghijkl"));
    }
}

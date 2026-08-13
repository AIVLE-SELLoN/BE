package com.aivle.sellon.domain.channels.service.connection;

import org.springframework.stereotype.Component;

/**
 * 쿠팡/지그재그 전용 API 키 형식 검증.
 */
@Component
public class ChannelKeyValidator {

    // TODO: 실제 발급 규칙 확정되면 접두사/길이 재확인 필요
    public boolean validateFormat(String channelType, String channelCode) {
        if (channelType == null || channelCode == null) {
            return false;
        }
        return switch (channelType) {
            case "COUPANG" -> channelCode.matches("^cp_live_[A-Za-z0-9]{12,}$");
            case "ZIGZAG" -> channelCode.matches("^zg_live_[A-Za-z0-9]{12,}$");
            default -> false;
        };
    }
}

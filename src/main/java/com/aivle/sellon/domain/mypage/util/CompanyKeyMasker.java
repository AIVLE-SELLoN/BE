package com.aivle.sellon.domain.mypage.util;

import org.springframework.stereotype.Component;

@Component
public class CompanyKeyMasker {

    private static final String PREFIX = "SLN-";
    private static final int VISIBLE_LENGTH = 4;

    public String mask(String joinKey) {
        if (joinKey == null) return null;

        String body = joinKey.startsWith(PREFIX) ? joinKey.substring(PREFIX.length()) : joinKey;
        if (body.length() <= VISIBLE_LENGTH) return PREFIX + body;

        return PREFIX
                + "•".repeat(body.length() - VISIBLE_LENGTH)
                + body.substring(body.length() - VISIBLE_LENGTH);
    }
}

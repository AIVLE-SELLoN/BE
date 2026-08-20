package com.aivle.sellon.domain.auth.util;

import org.springframework.stereotype.Component;

/** 아이디/비밀번호 찾기 결과로 화면에 노출되는 이메일을 마스킹한다. 예: exam***@sellon.com */
@Component
public class EmailMasker {

    private static final int VISIBLE_LENGTH = 3;

    public String mask(String email) {
        if (email == null) return null;

        int at = email.indexOf('@');
        if (at <= 0) return email;

        String local = email.substring(0, at);
        String domain = email.substring(at + 1);
        String visible = local.length() > VISIBLE_LENGTH ? local.substring(0, VISIBLE_LENGTH) : local;

        return visible + "***@" + domain;
    }
}

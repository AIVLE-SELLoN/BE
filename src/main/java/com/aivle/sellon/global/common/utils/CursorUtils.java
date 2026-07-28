package com.aivle.sellon.global.common.utils;

import com.aivle.sellon.global.common.exception.InvalidCursorException;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.util.Base64;

@Component
public class CursorUtils {

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH_BITS = 128;

    @Value("${spring.cursor.secret}")
    private String secret;

    private final SecureRandom secureRandom = new SecureRandom();
    private SecretKeySpec key;

    //secret -> key 에 미리 저장해 두는 로직, 등록 시 @PostConstruct 에 의해 사전에 미리 실행됨
    @PostConstruct
    public void init() {
        this.key = new SecretKeySpec(Base64.getDecoder().decode(secret), "AES");
    }

    //id -> cursor_encrypt
    public String toCursor(Long id) {
        if (id == null) return null;

        try {
            byte[] iv = new byte[GCM_IV_LENGTH];
            secureRandom.nextBytes(iv);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv));

            byte[] encrypted = cipher.doFinal(ByteBuffer.allocate(Long.BYTES).putLong(id).array());

            ByteBuffer buffer = ByteBuffer.allocate(iv.length + encrypted.length);
            buffer.put(iv).put(encrypted);

            return Base64.getUrlEncoder().withoutPadding().encodeToString(buffer.array());
        } catch (Exception e) {
            throw new InvalidCursorException();
        }
    }

    //cursor_encrypt -> id
    public Long toId(String cursor) {
        if (cursor == null) return null;

        try {
            byte[] decoded = Base64.getUrlDecoder().decode(cursor);

            byte[] iv = new byte[GCM_IV_LENGTH];
            byte[] encrypted = new byte[decoded.length - GCM_IV_LENGTH];
            System.arraycopy(decoded, 0, iv, 0, GCM_IV_LENGTH);
            System.arraycopy(decoded, GCM_IV_LENGTH, encrypted, 0, encrypted.length);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv));

            return ByteBuffer.wrap(cipher.doFinal(encrypted)).getLong();
        } catch (Exception e) {
            throw new InvalidCursorException();
        }
    }
}

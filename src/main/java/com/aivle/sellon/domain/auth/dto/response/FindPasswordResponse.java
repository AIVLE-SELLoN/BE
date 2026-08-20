package com.aivle.sellon.domain.auth.dto.response;

public record FindPasswordResponse(
        String maskedEmail
) {
    public static FindPasswordResponse of(String maskedEmail) {
        return new FindPasswordResponse(maskedEmail);
    }
}

package com.aivle.sellon.domain.auth.dto.response;

public record FindIdResponse(
        String maskedEmail
) {
    public static FindIdResponse of(String maskedEmail) {
        return new FindIdResponse(maskedEmail);
    }
}

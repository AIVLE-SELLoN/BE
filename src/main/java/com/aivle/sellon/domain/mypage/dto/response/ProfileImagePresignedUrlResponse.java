package com.aivle.sellon.domain.mypage.dto.response;

public record ProfileImagePresignedUrlResponse(
        String presignedUrl,
        String objectKey,
        String contentType,
        long expiresInSeconds
) {
    public static ProfileImagePresignedUrlResponse of(
            String presignedUrl,
            String objectKey,
            String contentType,
            long expiresInSeconds
    ) {
        return new ProfileImagePresignedUrlResponse(presignedUrl, objectKey, contentType, expiresInSeconds);
    }
}

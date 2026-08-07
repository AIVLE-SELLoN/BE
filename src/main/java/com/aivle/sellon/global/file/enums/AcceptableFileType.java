package com.aivle.sellon.global.file.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;

@Getter
@RequiredArgsConstructor
public enum AcceptableFileType {
    PDF("pdf" , "application/pdf"),
    JPG("jpg" , "image/jpeg") ,
    JPEG("jpeg" , "image/jpeg"),
    PNG("png" , "image/png");

    private final String extension;
    private final String mimeType;

    //확장자 검증 로직
    public static AcceptableFileType fromExtension(String extension) {
        return Arrays.stream(values())
                .filter(has -> has.extension.equalsIgnoreCase(extension))
                .findFirst().orElse(null);
    }
}

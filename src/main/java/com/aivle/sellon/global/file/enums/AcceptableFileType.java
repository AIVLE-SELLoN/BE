package com.aivle.sellon.global.file.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;

@Getter
@RequiredArgsConstructor
public enum AcceptableFileType {
    PDF("pdf" , "report/pdf"),
    JPG("jpg" , "image/jpg") ,
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

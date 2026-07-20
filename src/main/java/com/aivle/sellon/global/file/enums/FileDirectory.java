package com.aivle.sellon.global.file.enums;

import com.aivle.sellon.global.file.exception.ExtensionEmptyException;
import com.aivle.sellon.global.file.exception.InvalidDirectoryException;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import java.time.Duration;
import java.util.Arrays;

@Getter
@RequiredArgsConstructor
public enum FileDirectory {
    PROFILE("profile", Duration.ofMinutes(3) , 3 * 1024 * 1024L) ,
    REPORT("report", Duration.ofMinutes(10) , 10 * 1024 * 1024L);

    private final String prefix;
    private final Duration uploadTime;  //  presigned-url 만료시간 작성
    private final long fileSize;

    public static FileDirectory of(String inputPrefix) {
        if (inputPrefix == null || inputPrefix.isEmpty())
            throw new ExtensionEmptyException();


        return Arrays.stream(values())
                .filter(dir -> dir.prefix.equalsIgnoreCase(inputPrefix))
                .findFirst()
                .orElseThrow(InvalidDirectoryException::new);

    }
}

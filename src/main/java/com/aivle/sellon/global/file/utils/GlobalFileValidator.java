package com.aivle.sellon.global.file.utils;

import com.aivle.sellon.global.file.enums.AcceptableFileType;
import com.aivle.sellon.global.file.exception.InvalidExtensionException;
import org.springframework.stereotype.Component;

@Component
public class GlobalFileValidator {
    public AcceptableFileType validateAndGetExtension(String fileName) {
        String extension = extractExtension(fileName);

        AcceptableFileType fileType = AcceptableFileType.fromExtension(extension);
        if (fileType == null)
            throw new InvalidExtensionException();

        return fileType;
    }

    private String extractExtension(String fileName) {
        int lastDotIndex = fileName.lastIndexOf('.');
        if (lastDotIndex == -1) {
            throw new InvalidExtensionException();
        }

        return fileName.substring(lastDotIndex + 1);
    }
}
package com.aivle.sellon.global.file.service;

import com.aivle.sellon.global.file.enums.AcceptableFileType;
import com.aivle.sellon.global.file.enums.FileDirectory;
import com.aivle.sellon.global.file.exception.InvalidExtensionException;
import com.aivle.sellon.global.file.exception.InvalidFileException;
import com.aivle.sellon.global.file.utils.GlobalFileValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "file.storage-type", havingValue = "local", matchIfMissing = true)
public class LocalFileStorageService implements FileStorageService {

    private final GlobalFileValidator globalFileValidator;

    @Value("${file.upload-dir}")
    private String uploadDir;

    @Override
    public String store(MultipartFile file, FileDirectory directory) {
        if (file == null || file.isEmpty())
            throw new InvalidFileException();

        if (file.getSize() > directory.getFileSize())
            throw new InvalidFileException();

        // 확장자 검증 (비어있거나 인식 불가능한 확장자면 예외 발생)
        AcceptableFileType fileType = globalFileValidator.validateAndGetExtension(file.getOriginalFilename());

        // 해당 디렉토리에서 허용하지 않는 파일 타입이면 예외 발생 (예: PROFILE에 PDF 업로드 시도)
        if (!directory.allows(fileType))
            throw new InvalidExtensionException();

        try {
            Path dirPath = Paths.get(uploadDir, directory.getPrefix());
            Files.createDirectories(dirPath);

            String storedFilename = UUID.randomUUID() + extractExtension(file.getOriginalFilename());
            Path targetPath = dirPath.resolve(storedFilename);
            Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);

            return "/files/" + directory.getPrefix() + "/" + storedFilename;
        } catch (IOException e) {
            throw new InvalidFileException();
        }
    }

    @Override
    public String getAccessUrl(String storedKey, FileDirectory directory) {
        // 로컬 저장은 store()가 이미 접근 가능한 경로를 반환하므로 그대로 반환
        return storedKey;
    }

    private String extractExtension(String originalFilename) {
        if (originalFilename == null || !originalFilename.contains("."))
            return "";
        return originalFilename.substring(originalFilename.lastIndexOf("."));
    }
}

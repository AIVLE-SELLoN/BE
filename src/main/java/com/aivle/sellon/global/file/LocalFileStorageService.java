package com.aivle.sellon.global.file;

import com.aivle.sellon.global.file.exception.InvalidFileException;
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
@ConditionalOnProperty(name = "file.storage-type", havingValue = "local", matchIfMissing = true)
public class LocalFileStorageService implements FileStorageService {

    @Value("${file.upload-dir}")
    private String uploadDir;

    @Override
    public String store(MultipartFile file) {
        if (file == null || file.isEmpty())
            throw new InvalidFileException();

        try {
            Path dirPath = Paths.get(uploadDir);
            Files.createDirectories(dirPath);

            String storedFilename = UUID.randomUUID() + extractExtension(file.getOriginalFilename());
            Path targetPath = dirPath.resolve(storedFilename);
            Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);

            return "/files/" + storedFilename;
        } catch (IOException e) {
            throw new InvalidFileException();
        }
    }

    @Override
    public String getAccessUrl(String storedKey) {
        // 로컬 저장은 store()가 이미 접근 가능한 경로를 반환하므로 그대로 반환
        return storedKey;
    }

    private String extractExtension(String originalFilename) {
        if (originalFilename == null || !originalFilename.contains("."))
            return "";
        return originalFilename.substring(originalFilename.lastIndexOf("."));
    }
}

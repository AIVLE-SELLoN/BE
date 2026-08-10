package com.aivle.sellon.global.file;

import com.aivle.sellon.global.file.dto.FileUploadResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/files")
@RequiredArgsConstructor
public class FileController {

    private final FileStorageService fileStorageService;

    @PostMapping
    public ResponseEntity<FileUploadResponse> upload(@RequestParam("file") MultipartFile file) {
        String fileUrl = fileStorageService.store(file);
        return ResponseEntity.ok(new FileUploadResponse(fileUrl));
    }
}

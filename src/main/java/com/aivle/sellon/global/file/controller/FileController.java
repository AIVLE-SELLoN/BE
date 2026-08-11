package com.aivle.sellon.global.file.controller;

import com.aivle.sellon.global.file.dto.FileUploadResponse;
import com.aivle.sellon.global.file.enums.FileDirectory;
import com.aivle.sellon.global.file.service.FileStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
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

    @PostMapping("/{directory}")
    public ResponseEntity<FileUploadResponse> upload(
            @PathVariable String directory,
            @RequestParam("file") MultipartFile file
    ) {
        FileDirectory fileDirectory = FileDirectory.of(directory);
        String fileUrl = fileStorageService.store(file, fileDirectory);
        return ResponseEntity.ok(new FileUploadResponse(fileUrl));
    }
}

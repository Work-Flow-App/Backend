package com.workflow.controller.file;

import com.workflow.service.storage.FileStorageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/files")
@RequiredArgsConstructor
public class FileController {

    private final FileStorageService fileStorageService;

    @Operation(summary = "Upload an image file", description = "Upload an image file to S3 storage. Supports JPEG, PNG, GIF, and WebP formats. Maximum file size: 10MB")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "File uploaded successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid file or file type"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, String>> uploadFile(
            @Parameter(description = "Image file to upload", required = true)
            @RequestParam("file") MultipartFile file) {

        try {
            if (file.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "File is empty"));
            }

            String contentType = file.getContentType();
            if (contentType == null || !isValidImageType(contentType)) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Only image files are allowed (JPEG, PNG, GIF, WebP)"));
            }

            long maxFileSize = 10 * 1024 * 1024;
            if (file.getSize() > maxFileSize) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "File size exceeds maximum limit of 10MB"));
            }

            log.info("Uploading file: name={}, size={}, type={}",
                     file.getOriginalFilename(), file.getSize(), file.getContentType());

            String fileKey = fileStorageService.uploadFile(file);
            String presignedUrl = fileStorageService.generatePresignedUrl(fileKey, Duration.ofHours(1));

            Map<String, String> response = new HashMap<>();
            response.put("message", "File uploaded successfully");
            response.put("fileKey", fileKey);
            response.put("url", presignedUrl);
            response.put("originalFileName", file.getOriginalFilename());
            response.put("fileSize", String.valueOf(file.getSize()));
            response.put("contentType", file.getContentType());

            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (Exception e) {
            log.error("Error uploading file", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to upload file: " + e.getMessage()));
        }
    }

    private boolean isValidImageType(String contentType) {
        return contentType.equals("image/jpeg") ||
               contentType.equals("image/png") ||
               contentType.equals("image/gif") ||
               contentType.equals("image/webp");
    }
}
package com.antiplagiarism.filestorage.controller;

import com.antiplagiarism.filestorage.entity.FileEntity;
import com.antiplagiarism.filestorage.service.FileStorageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/files")
@Tag(name = "File Storing Service", description = "Manages file storage and retrieval")
public class FileController {

    private final FileStorageService storageService;

    public FileController(FileStorageService storageService) {
        this.storageService = storageService;
    }

    @PostMapping
    @Operation(summary = "Upload a file", description = "Store a file and return its ID")
    public ResponseEntity<?> uploadFile(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body("File is empty");
        }

        try {
            FileEntity saved = storageService.storeFile(file);
            
            Map<String, Object> result = new HashMap<>();
            result.put("fileId", saved.getId());
            result.put("filename", saved.getFilename());
            result.put("size", saved.getFileSize());
            result.put("uploadedAt", saved.getUploadedAt());
            
            return ResponseEntity.ok(result);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to store file: " + e.getMessage());
        }
    }

    @GetMapping("/{fileId}")
    @Operation(summary = "Get file metadata", description = "Retrieve file information by ID")
    public ResponseEntity<?> getFile(@PathVariable String fileId) {
        try {
            FileEntity entity = storageService.getFile(fileId);
            
            Map<String, Object> result = new HashMap<>();
            result.put("fileId", entity.getId());
            result.put("filename", entity.getFilename());
            result.put("contentType", entity.getContentType());
            result.put("size", entity.getFileSize());
            result.put("uploadedAt", entity.getUploadedAt());
            
            return ResponseEntity.ok(result);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("File not found: " + e.getMessage());
        }
    }

    @GetMapping("/{fileId}/content")
    @Operation(summary = "Get file content", description = "Retrieve file content as text")
    public ResponseEntity<?> getFileContent(@PathVariable String fileId) {
        try {
            String content = storageService.getFileContent(fileId);
            
            Map<String, String> result = new HashMap<>();
            result.put("fileId", fileId);
            result.put("content", content);
            
            return ResponseEntity.ok(result);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("File not found: " + e.getMessage());
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to read file: " + e.getMessage());
        }
    }

    @GetMapping("/health")
    @Operation(summary = "Health check", description = "Check if File Storing Service is running")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("File Storing Service is running");
    }
}

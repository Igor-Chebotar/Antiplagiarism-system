package com.antiplagiarism.filestorage.service;

import com.antiplagiarism.filestorage.entity.FileEntity;
import com.antiplagiarism.filestorage.repository.FileRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
public class FileStorageService {

    private final FileRepository fileRepo;
    private final Path storagePath;

    public FileStorageService(FileRepository fileRepo,
                              @Value("${file.storage.location}") String location) {
        this.fileRepo = fileRepo;
        this.storagePath = Paths.get(location).toAbsolutePath().normalize();
        
        try {
            Files.createDirectories(this.storagePath);
        } catch (Exception e) {
            throw new RuntimeException("Could not create upload directory", e);
        }
    }

    public FileEntity storeFile(MultipartFile file) throws IOException {
        String originalName = file.getOriginalFilename();
        String fileId = UUID.randomUUID().toString();
        String storedName = fileId + "_" + originalName;

        Path target = this.storagePath.resolve(storedName);
        Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);

        FileEntity entity = new FileEntity();
        entity.setId(fileId);
        entity.setFilename(originalName);
        entity.setFilePath(target.toString());
        entity.setContentType(file.getContentType());
        entity.setFileSize(file.getSize());

        return fileRepo.save(entity);
    }

    public FileEntity getFile(String fileId) {
        return fileRepo.findById(fileId)
                .orElseThrow(() -> new RuntimeException("File not found: " + fileId));
    }

    public String getFileContent(String fileId) throws IOException {
        FileEntity entity = getFile(fileId);
        return Files.readString(Paths.get(entity.getFilePath()));
    }
}

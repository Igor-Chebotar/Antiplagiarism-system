package com.antiplagiarism.filestorage.repository;

import com.antiplagiarism.filestorage.entity.FileEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FileRepository extends JpaRepository<FileEntity, String> {
}

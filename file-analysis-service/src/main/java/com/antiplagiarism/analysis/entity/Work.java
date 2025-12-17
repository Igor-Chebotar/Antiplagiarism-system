package com.antiplagiarism.analysis.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "works")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Work {

    @Id
    private String id;

    @Column(nullable = false)
    private String studentName;

    @Column(nullable = false)
    private String assignmentId;

    @Column(nullable = false)
    private String fileId;

    @Column(nullable = false)
    private LocalDateTime submittedAt;

    @PrePersist
    public void prePersist() {
        if (id == null) {
            id = UUID.randomUUID().toString();
        }
        if (submittedAt == null) {
            submittedAt = LocalDateTime.now();
        }
    }
}

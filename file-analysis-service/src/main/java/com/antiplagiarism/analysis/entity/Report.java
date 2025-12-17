package com.antiplagiarism.analysis.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "reports")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Report {

    @Id
    private String id;

    @Column(nullable = false)
    private String workId;

    @Column(nullable = false)
    private String status; // PENDING, COMPLETED, FAILED

    private Boolean plagiarismDetected;

    private Double originalityPercent;

    private String verdict; // ORIGINAL, SUSPICIOUS, PLAGIARISM

    @Column(columnDefinition = "TEXT")
    private String details; // JSON string with match details

    @Column(nullable = false)
    private LocalDateTime createdAt;

    private LocalDateTime completedAt;

    @PrePersist
    public void prePersist() {
        if (id == null) {
            id = UUID.randomUUID().toString();
        }
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (status == null) {
            status = "PENDING";
        }
    }
}

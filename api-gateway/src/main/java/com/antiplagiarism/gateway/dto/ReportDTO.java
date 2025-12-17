package com.antiplagiarism.gateway.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ReportDTO {
    private String id;
    private String workId;
    private String status;
    private Boolean plagiarismDetected;
    private Double originalityPercent;
    private String verdict;
    private String details;
    private LocalDateTime createdAt;
    private LocalDateTime completedAt;
}

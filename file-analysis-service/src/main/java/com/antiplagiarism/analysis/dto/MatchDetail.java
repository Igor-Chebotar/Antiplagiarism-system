package com.antiplagiarism.analysis.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MatchDetail {
    private String matchedWorkId;
    private String studentName;
    private Double similarityPercent;
    private String submittedAt;
    private String verdict;
}

package com.antiplagiarism.analysis.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PlagiarismResult {
    private Double originalityPercent;
    private Boolean plagiarismDetected;
    private String verdict;
    private List<MatchDetail> matches;
}

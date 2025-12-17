package com.antiplagiarism.gateway.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class WorkSubmissionResponse {
    private String workId;
    private String message;
    private String status;
}

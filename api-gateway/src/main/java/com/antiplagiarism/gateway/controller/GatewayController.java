package com.antiplagiarism.gateway.controller;

import com.antiplagiarism.gateway.dto.ReportDTO;
import com.antiplagiarism.gateway.dto.WorkSubmissionResponse;
import com.antiplagiarism.gateway.service.GatewayService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api")
@Tag(name = "API Gateway", description = "Central entry point for all client requests")
public class GatewayController {

    private final GatewayService gatewayService;

    public GatewayController(GatewayService gatewayService) {
        this.gatewayService = gatewayService;
    }

    @PostMapping("/works")
    @Operation(summary = "Submit work for plagiarism check", 
               description = "Student submits a work file along with metadata")
    public ResponseEntity<?> submitWork(
            @RequestParam("file") MultipartFile file,
            @RequestParam("studentName") String studentName,
            @RequestParam("assignmentId") String assignmentId) {
        
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body("File is empty");
        }

        if (!file.getOriginalFilename().endsWith(".txt")) {
            return ResponseEntity.badRequest().body("Only .txt files are supported");
        }

        try {
            WorkSubmissionResponse resp = gatewayService.submitWork(file, studentName, assignmentId);
            return ResponseEntity.ok(resp);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to process file: " + e.getMessage());
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body("Service temporarily unavailable: " + e.getMessage());
        }
    }

    @GetMapping("/works/{workId}/reports")
    @Operation(summary = "Get reports for a work", 
               description = "Teacher retrieves plagiarism reports for a specific work")
    public ResponseEntity<?> getReports(@PathVariable String workId) {
        try {
            List<ReportDTO> reportsList = gatewayService.getReports(workId);
            return ResponseEntity.ok(reportsList);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body("Service temporarily unavailable: " + e.getMessage());
        }
    }

    @GetMapping("/works/{workId}/wordcloud")
    @Operation(summary = "Get word cloud for a work", 
               description = "Returns word frequency data for visualization as word cloud")
    public ResponseEntity<?> getWordCloud(
            @PathVariable String workId,
            @RequestParam(defaultValue = "30") int maxWords) {
        try {
            Object wordCloudData = gatewayService.getWordCloud(workId, maxWords);
            return ResponseEntity.ok(wordCloudData);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body("Service temporarily unavailable: " + e.getMessage());
        }
    }

    @GetMapping("/health")
    @Operation(summary = "Health check", description = "Check if API Gateway is running")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("API Gateway is running");
    }
}

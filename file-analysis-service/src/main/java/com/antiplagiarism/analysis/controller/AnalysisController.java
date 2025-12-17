package com.antiplagiarism.analysis.controller;

import com.antiplagiarism.analysis.entity.Report;
import com.antiplagiarism.analysis.entity.Work;
import com.antiplagiarism.analysis.service.AnalysisService;
import com.antiplagiarism.analysis.service.WordCloudService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/analysis")
@Tag(name = "File Analysis Service", description = "Manages plagiarism analysis and reports")
public class AnalysisController {

    private final AnalysisService analysisService;
    private final WordCloudService wordCloudService;

    public AnalysisController(AnalysisService analysisService, WordCloudService wordCloudService) {
        this.analysisService = analysisService;
        this.wordCloudService = wordCloudService;
    }

    @PostMapping
    @Operation(summary = "Create work and start analysis", 
               description = "Register a new work submission and initiate plagiarism check")
    public ResponseEntity<?> createWork(@RequestBody Map<String, String> request) {
        String fileId = request.get("fileId");
        String studentName = request.get("studentName");
        String assignmentId = request.get("assignmentId");

        if (fileId == null || studentName == null || assignmentId == null) {
            return ResponseEntity.badRequest().body("Missing required fields");
        }

        try {
            Work work = analysisService.createWorkAndAnalyze(fileId, studentName, assignmentId);
            
            Map<String, Object> resp = new HashMap<>();
            resp.put("workId", work.getId());
            resp.put("studentName", work.getStudentName());
            resp.put("assignmentId", work.getAssignmentId());
            resp.put("submittedAt", work.getSubmittedAt());
            resp.put("status", "Analysis started");
            
            return ResponseEntity.ok(resp);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body("Failed to start analysis: " + e.getMessage());
        }
    }

    @GetMapping("/reports/{workId}")
    @Operation(summary = "Get reports for a work", 
               description = "Retrieve all plagiarism reports for a specific work")
    public ResponseEntity<?> getReports(@PathVariable String workId) {
        try {
            List<Report> reports = analysisService.getReportsByWorkId(workId);
            if (reports.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok(reports);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to fetch reports: " + e.getMessage());
        }
    }

    @GetMapping("/reports/work/{reportId}")
    @Operation(summary = "Get specific report", description = "Retrieve a single report by its ID")
    public ResponseEntity<?> getReport(@PathVariable String reportId) {
        try {
            Report report = analysisService.getReportById(reportId);
            return ResponseEntity.ok(report);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Report not found: " + e.getMessage());
        }
    }

    @GetMapping("/wordcloud/{workId}")
    @Operation(summary = "Generate word cloud for a work", 
               description = "Returns word frequency data for visualization")
    public ResponseEntity<?> getWordCloud(
            @PathVariable String workId,
            @RequestParam(defaultValue = "30") int maxWords) {
        try {
            String content = analysisService.getWorkContent(workId);
            if (content == null || content.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Work content not found");
            }

            WordCloudService.WordCloudResult result = wordCloudService.generateWordCloudWithSizes(content, maxWords);

            Map<String, Object> resp = new HashMap<>();
            resp.put("workId", workId);
            resp.put("wordCloud", result.getWords());
            resp.put("totalWordsAnalyzed", result.getTotalWordsAnalyzed());
            resp.put("uniqueWords", result.getWords().size());

            return ResponseEntity.ok(resp);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to generate word cloud: " + e.getMessage());
        }
    }

    @GetMapping("/health")
    @Operation(summary = "Health check", description = "Check if File Analysis Service is running")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("File Analysis Service is running");
    }
}

package com.antiplagiarism.analysis.service;

import com.antiplagiarism.analysis.dto.PlagiarismResult;
import com.antiplagiarism.analysis.entity.Report;
import com.antiplagiarism.analysis.entity.Work;
import com.antiplagiarism.analysis.repository.ReportRepository;
import com.antiplagiarism.analysis.repository.WorkRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class AnalysisService {

    private final WorkRepository workRepo;
    private final ReportRepository reportRepo;
    private final PlagiarismDetectionService plagiarismService;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${file.storing.service.url}")
    private String fileStoringUrl;

    public AnalysisService(WorkRepository workRepo, ReportRepository reportRepo,
                          PlagiarismDetectionService plagiarismService,
                          RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.workRepo = workRepo;
        this.reportRepo = reportRepo;
        this.plagiarismService = plagiarismService;
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    public Work createWorkAndAnalyze(String fileId, String studentName, String assignmentId) {
        Work work = new Work();
        work.setFileId(fileId);
        work.setStudentName(studentName);
        work.setAssignmentId(assignmentId);
        work = workRepo.save(work);

        Report report = new Report();
        report.setWorkId(work.getId());
        report.setStatus("PENDING");
        report = reportRepo.save(report);

        try {
            runAnalysis(work, report);
        } catch (Exception e) {
            report.setStatus("FAILED");
            report.setCompletedAt(LocalDateTime.now());
            reportRepo.save(report);
        }

        return work;
    }

    private void runAnalysis(Work currentWork, Report report) {
        try {
            String currentContent = getFileContent(currentWork.getFileId());

            List<Work> otherWorks = workRepo.findByAssignmentIdOrderBySubmittedAtAsc(currentWork.getAssignmentId());
            otherWorks.removeIf(w -> w.getId().equals(currentWork.getId()));

            List<PlagiarismDetectionService.WorkContentPair> previousWorks = new ArrayList<>();
            for (Work other : otherWorks) {
                if (other.getSubmittedAt().isBefore(currentWork.getSubmittedAt())) {
                    try {
                        String content = getFileContent(other.getFileId());
                        previousWorks.add(new PlagiarismDetectionService.WorkContentPair(
                                other.getId(), other.getStudentName(),
                                other.getSubmittedAt().toString(), content
                        ));
                    } catch (Exception ignored) {}
                }
            }

            PlagiarismResult result = plagiarismService.analyzePlagiarism(currentContent, previousWorks);

            report.setStatus("COMPLETED");
            report.setPlagiarismDetected(result.getPlagiarismDetected());
            report.setOriginalityPercent(result.getOriginalityPercent());
            report.setVerdict(result.getVerdict());
            report.setDetails(objectMapper.writeValueAsString(result.getMatches()));
            report.setCompletedAt(LocalDateTime.now());
            reportRepo.save(report);

        } catch (Exception e) {
            report.setStatus("FAILED");
            report.setCompletedAt(LocalDateTime.now());
            reportRepo.save(report);
            throw new RuntimeException("Analysis failed", e);
        }
    }

    private String getFileContent(String fileId) {
        try {
            ResponseEntity<Map> resp = restTemplate.exchange(
                    fileStoringUrl + "/files/" + fileId + "/content",
                    HttpMethod.GET, null, Map.class
            );
            if (resp.getBody() != null) {
                return (String) resp.getBody().get("content");
            }
            throw new RuntimeException("Empty response");
        } catch (Exception e) {
            throw new RuntimeException("File Storing Service unavailable", e);
        }
    }

    public String getWorkContent(String workId) {
        Work work = workRepo.findById(workId)
                .orElseThrow(() -> new RuntimeException("Work not found"));
        return getFileContent(work.getFileId());
    }

    public List<Report> getReportsByWorkId(String workId) {
        return reportRepo.findByWorkId(workId);
    }

    public Report getReportById(String reportId) {
        return reportRepo.findById(reportId)
                .orElseThrow(() -> new RuntimeException("Report not found"));
    }
}

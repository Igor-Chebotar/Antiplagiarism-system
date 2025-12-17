package com.antiplagiarism.gateway.service;

import com.antiplagiarism.gateway.dto.ReportDTO;
import com.antiplagiarism.gateway.dto.WorkSubmissionResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class GatewayService {

    private final RestTemplate restTemplate;

    @Value("${file.storing.service.url}")
    private String fileStoringUrl;

    @Value("${file.analysis.service.url}")
    private String analysisUrl;

    public GatewayService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public WorkSubmissionResponse submitWork(MultipartFile file, String studentName, String assignmentId) throws IOException {
        String fileId = uploadFile(file);
        String workId = createWork(fileId, studentName, assignmentId);
        return new WorkSubmissionResponse(workId, "Work submitted successfully", "PROCESSING");
    }

    private String uploadFile(MultipartFile file) throws IOException {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", new ByteArrayResource(file.getBytes()) {
            @Override
            public String getFilename() {
                return file.getOriginalFilename();
            }
        });

        HttpEntity<MultiValueMap<String, Object>> req = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<Map> resp = restTemplate.postForEntity(fileStoringUrl + "/files", req, Map.class);
            if (resp.getStatusCode() == HttpStatus.OK && resp.getBody() != null) {
                return (String) resp.getBody().get("fileId");
            }
            throw new RuntimeException("Failed to upload file");
        } catch (RestClientException e) {
            throw new RuntimeException("File Storing Service unavailable", e);
        }
    }

    private String createWork(String fileId, String studentName, String assignmentId) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, String> body = new HashMap<>();
        body.put("fileId", fileId);
        body.put("studentName", studentName);
        body.put("assignmentId", assignmentId);

        HttpEntity<Map<String, String>> req = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<Map> resp = restTemplate.postForEntity(analysisUrl + "/analysis", req, Map.class);
            if (resp.getStatusCode() == HttpStatus.OK && resp.getBody() != null) {
                return (String) resp.getBody().get("workId");
            }
            throw new RuntimeException("Failed to create work");
        } catch (RestClientException e) {
            throw new RuntimeException("Analysis Service unavailable", e);
        }
    }

    public List<ReportDTO> getReports(String workId) {
        try {
            ResponseEntity<List<ReportDTO>> resp = restTemplate.exchange(
                    analysisUrl + "/analysis/reports/" + workId,
                    HttpMethod.GET, null,
                    new ParameterizedTypeReference<List<ReportDTO>>() {}
            );
            if (resp.getStatusCode() == HttpStatus.OK) {
                return resp.getBody();
            }
            throw new RuntimeException("Failed to fetch reports");
        } catch (RestClientException e) {
            throw new RuntimeException("Analysis Service unavailable", e);
        }
    }

    public Object getWordCloud(String workId, int maxWords) {
        try {
            ResponseEntity<Map> resp = restTemplate.exchange(
                    analysisUrl + "/analysis/wordcloud/" + workId + "?maxWords=" + maxWords,
                    HttpMethod.GET, null, Map.class
            );
            if (resp.getStatusCode() == HttpStatus.OK) {
                return resp.getBody();
            }
            throw new RuntimeException("Failed to get word cloud");
        } catch (RestClientException e) {
            throw new RuntimeException("Analysis Service unavailable", e);
        }
    }
}

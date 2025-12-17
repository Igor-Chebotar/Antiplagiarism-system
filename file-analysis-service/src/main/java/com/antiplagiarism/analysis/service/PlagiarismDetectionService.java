package com.antiplagiarism.analysis.service;

import com.antiplagiarism.analysis.dto.MatchDetail;
import com.antiplagiarism.analysis.dto.PlagiarismResult;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class PlagiarismDetectionService {

    // similarity = (2 * LCS) / (len1 + len2) * 100
    public double calculateSimilarity(String text1, String text2) {
        if (text1 == null || text2 == null || text1.isEmpty() || text2.isEmpty()) {
            return 0.0;
        }

        String norm1 = normalize(text1);
        String norm2 = normalize(text2);

        int lcs = lcs(norm1, norm2);
        double similarity = (2.0 * lcs) / (norm1.length() + norm2.length()) * 100;

        return Math.round(similarity * 100.0) / 100.0;
    }

    private String normalize(String text) {
        return text.toLowerCase().replaceAll("\\s+", " ").trim();
    }

    // longest common subsequence - dynamic programming
    private int lcs(String a, String b) {
        int m = a.length();
        int n = b.length();
        int[][] dp = new int[m + 1][n + 1];

        for (int i = 1; i <= m; i++) {
            for (int j = 1; j <= n; j++) {
                if (a.charAt(i - 1) == b.charAt(j - 1)) {
                    dp[i][j] = dp[i - 1][j - 1] + 1;
                } else {
                    dp[i][j] = Math.max(dp[i - 1][j], dp[i][j - 1]);
                }
            }
        }
        return dp[m][n];
    }

    public String getVerdict(double similarity) {
        if (similarity >= 80.0) return "PLAGIARISM";
        if (similarity >= 50.0) return "SUSPICIOUS";
        return "ORIGINAL";
    }

    public PlagiarismResult analyzePlagiarism(String currentContent, List<WorkContentPair> previousWorks) {
        List<MatchDetail> matches = new ArrayList<>();
        double maxSimilarity = 0.0;
        String verdict = "ORIGINAL";

        for (WorkContentPair prev : previousWorks) {
            double sim = calculateSimilarity(currentContent, prev.getContent());

            if (sim >= 50.0) {
                MatchDetail match = new MatchDetail();
                match.setMatchedWorkId(prev.getWorkId());
                match.setStudentName(prev.getStudentName());
                match.setSimilarityPercent(sim);
                match.setSubmittedAt(prev.getSubmittedAt());
                match.setVerdict(getVerdict(sim));
                matches.add(match);

                if (sim > maxSimilarity) {
                    maxSimilarity = sim;
                    verdict = getVerdict(sim);
                }
            }
        }

        PlagiarismResult result = new PlagiarismResult();
        result.setOriginalityPercent(Math.max(0, 100 - maxSimilarity));
        result.setPlagiarismDetected(maxSimilarity >= 50.0);
        result.setVerdict(verdict);
        result.setMatches(matches);

        return result;
    }

    public static class WorkContentPair {
        private String workId;
        private String studentName;
        private String submittedAt;
        private String content;

        public WorkContentPair(String workId, String studentName, String submittedAt, String content) {
            this.workId = workId;
            this.studentName = studentName;
            this.submittedAt = submittedAt;
            this.content = content;
        }

        public String getWorkId() { return workId; }
        public String getStudentName() { return studentName; }
        public String getSubmittedAt() { return submittedAt; }
        public String getContent() { return content; }
    }
}

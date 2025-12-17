package com.antiplagiarism.analysis.repository;

import com.antiplagiarism.analysis.entity.Report;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReportRepository extends JpaRepository<Report, String> {
    List<Report> findByWorkId(String workId);
}

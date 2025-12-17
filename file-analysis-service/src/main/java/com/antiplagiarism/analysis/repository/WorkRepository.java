package com.antiplagiarism.analysis.repository;

import com.antiplagiarism.analysis.entity.Work;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WorkRepository extends JpaRepository<Work, String> {
    List<Work> findByAssignmentId(String assignmentId);
    List<Work> findByAssignmentIdOrderBySubmittedAtAsc(String assignmentId);
}

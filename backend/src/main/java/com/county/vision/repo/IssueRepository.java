package com.county.vision.repo;

import com.county.vision.entity.Issue;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface IssueRepository extends JpaRepository<Issue, Long> {
    List<Issue> findByStudentIdOrderByCreatedAtDesc(Long studentId);
    List<Issue> findByStatusOrderByCreatedAtDesc(String status);
    List<Issue> findByIssueTypeAndStatus(String issueType, String status);
    long countByStatus(String status);
}

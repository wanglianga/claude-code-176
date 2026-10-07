package com.county.vision.domain.repository;

import com.county.vision.domain.entity.IssueTracker;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface IssueTrackerRepository extends JpaRepository<IssueTracker, Long> {
    List<IssueTracker> findByStatusOrderByRaisedAtDesc(String status);
    List<IssueTracker> findByStudentIdOrderByRaisedAtDesc(Long studentId);
    Optional<IssueTracker> findFirstByStudentIdAndIssueTypeAndStatus(Long studentId, String issueType, String status);
    long countByStatus(String status);
}

package com.county.vision.domain.repository;

import com.county.vision.domain.entity.ReviewFollowUp;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface ReviewFollowUpRepository extends JpaRepository<ReviewFollowUp, Long> {
    List<ReviewFollowUp> findByStudentIdOrderByPlannedDateAscIdAsc(Long studentId);
    List<ReviewFollowUp> findByStatusOrderByPlannedDateAsc(String status);
    List<ReviewFollowUp> findByStatusAndPlannedDateLessThanEqual(String status, LocalDate date);
}

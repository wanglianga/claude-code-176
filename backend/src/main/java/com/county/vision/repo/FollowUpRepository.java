package com.county.vision.repo;

import com.county.vision.entity.FollowUp;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.List;

public interface FollowUpRepository extends JpaRepository<FollowUp, Long> {
    List<FollowUp> findByStudentIdOrderByDueDateDesc(Long studentId);
    List<FollowUp> findByStatusAndDueDateBefore(String status, LocalDate date);
    List<FollowUp> findByStatusAndDueDateBetween(String status, LocalDate from, LocalDate to);
}

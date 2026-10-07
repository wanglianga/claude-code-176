package com.county.vision.domain.repository;

import com.county.vision.domain.entity.HealthEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface HealthEventRepository extends JpaRepository<HealthEvent, Long> {
    List<HealthEvent> findByStudentIdOrderByEventTimeAscIdAsc(Long studentId);
}

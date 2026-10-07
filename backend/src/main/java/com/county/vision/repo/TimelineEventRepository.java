package com.county.vision.repo;

import com.county.vision.entity.TimelineEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface TimelineEventRepository extends JpaRepository<TimelineEvent, Long> {
    List<TimelineEvent> findByStudentIdOrderByEventTimeDescIdDesc(Long studentId);
}

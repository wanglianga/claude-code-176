package com.county.vision.repo;

import com.county.vision.entity.Intervention;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface InterventionRepository extends JpaRepository<Intervention, Long> {
    List<Intervention> findByStudentIdOrderByCreatedAtDesc(Long studentId);
    List<Intervention> findBySchoolIdAndStatus(Long schoolId, String status);
    List<Intervention> findByClassIdOrderByCreatedAtDesc(Long classId);
    List<Intervention> findByTypeAndStatus(String type, String status);
}

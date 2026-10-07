package com.county.vision.domain.repository;

import com.county.vision.domain.entity.GradeClass;
import com.county.vision.domain.entity.School;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface GradeClassRepository extends JpaRepository<GradeClass, Long> {
    Optional<GradeClass> findBySchoolAndGradeAndClassName(School school, String grade, String className);
    List<GradeClass> findBySchoolIdOrderByGradeAscClassNameAsc(Long schoolId);
}

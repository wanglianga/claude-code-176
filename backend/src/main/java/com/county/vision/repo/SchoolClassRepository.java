package com.county.vision.repo;

import com.county.vision.entity.SchoolClass;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface SchoolClassRepository extends JpaRepository<SchoolClass, Long> {
    List<SchoolClass> findBySchoolIdOrderByGradeAscClassNameAsc(Long schoolId);
    List<SchoolClass> findBySchoolIdAndGrade(Long schoolId, String grade);
    List<SchoolClass> findByHeadTeacherId(Long headTeacherId);
}

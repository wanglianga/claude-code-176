package com.county.vision.domain.repository;

import com.county.vision.domain.entity.Student;
import com.county.vision.domain.enums.StudentStage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface StudentRepository extends JpaRepository<Student, Long> {
    Optional<Student> findByStudentNoAndScreenYear(String studentNo, Integer screenYear);
    List<Student> findBySchoolIdOrderByIdAsc(Long schoolId);
    long countByStage(StudentStage stage);

    @Query("select distinct s from Student s left join fetch s.gradeClass gc left join fetch s.school sch where " +
            "(:schoolId is null or sch.id = :schoolId) and " +
            "(:grade is null or gc.grade = :grade) and " +
            "(:regionId is null or sch.region.id = :regionId) and " +
            "(:stage is null or s.stage = :stage) and " +
            "(:keyword is null or s.name like %:keyword% or s.studentNo like %:keyword%) and " +
            "(:onlyImportError = false or s.importError = true) " +
            "order by s.id asc")
    List<Student> search(@Param("schoolId") Long schoolId,
                         @Param("grade") String grade,
                         @Param("regionId") Long regionId,
                         @Param("stage") StudentStage stage,
                         @Param("keyword") String keyword,
                         @Param("onlyImportError") boolean onlyImportError);

    @Query("select count(s) from Student s where s.flags like %:flag%")
    long countByFlag(@Param("flag") String flag);
}

package com.county.vision.repo;

import com.county.vision.entity.Student;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface StudentRepository extends JpaRepository<Student, Long> {
    List<Student> findByClassId(Long classId);
    List<Student> findBySchoolId(Long schoolId);
    long countBySchoolIdAndStatus(Long schoolId, String status);

    @Query("select s from Student s where (:schoolId is null or s.schoolId = :schoolId) " +
           "and (:classId is null or s.classId = :classId) " +
           "and (:grade is null or s.grade = :grade) " +
           "and (:status is null or s.status = :status) " +
           "and (:kw is null or s.name like %:kw% or s.studentNo like %:kw% or s.parentPhone like %:kw%) " +
           "order by s.updatedAt desc")
    Page<Student> search(@Param("schoolId") Long schoolId, @Param("classId") Long classId,
                         @Param("grade") String grade, @Param("status") String status,
                         @Param("kw") String kw, Pageable pageable);

    @Query("select s.grade, count(s) from Student s where s.schoolId = :schoolId group by s.grade")
    List<Object[]> countGroupByGrade(@Param("schoolId") Long schoolId);
}

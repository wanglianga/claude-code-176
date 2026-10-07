package com.county.vision.repo;

import com.county.vision.entity.Screening;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ScreeningRepository extends JpaRepository<Screening, Long> {
    List<Screening> findByBatchId(Long batchId);
    List<Screening> findByStudentIdOrderByScreeningTimeDesc(Long studentId);
    Optional<Screening> findFirstByStudentIdOrderByScreeningTimeDesc(Long studentId);
    long countByBatchIdAndResult(Long batchId, String result);
    long countByBatchIdAndAbsentTrue(Long batchId);
    long countByResult(String result);

    @Query(value = "select count(*) from screenings sc join screening_batches b on sc.batch_id=b.id " +
                   "where b.school_id=:schoolId and sc.absent=false", nativeQuery = true)
    long countScreenedBySchool(@Param("schoolId") Long schoolId);

    @Query(value = "select count(*) from screenings sc join screening_batches b on sc.batch_id=b.id " +
                   "where b.school_id=:schoolId and sc.result='ABNORMAL'", nativeQuery = true)
    long countAbnormalBySchool(@Param("schoolId") Long schoolId);

    @Query(value = "select to_char(b.screening_date,'YYYY-MM') m, count(*) total, " +
                   "sum(case when sc.result='ABNORMAL' then 1 else 0 end) abnormal " +
                   "from screenings sc join screening_batches b on sc.batch_id=b.id " +
                   "where sc.absent=false and (:schoolId is null or b.school_id=:schoolId) " +
                   "group by m order by m", nativeQuery = true)
    List<Object[]> monthlyTrend(@Param("schoolId") Long schoolId);

    @Query(value = "select coalesce(o.area,'未分区') area, count(*) total, " +
                   "sum(case when sc.result='ABNORMAL' then 1 else 0 end) abnormal " +
                   "from screenings sc join screening_batches b on sc.batch_id=b.id " +
                   "join organizations o on b.school_id=o.id " +
                   "where sc.absent=false group by area order by area", nativeQuery = true)
    List<Object[]> areaStats();

    @Query(value = "select s.grade grade, count(*) total, " +
                   "sum(case when sc.result='ABNORMAL' then 1 else 0 end) abnormal " +
                   "from screenings sc join students s on sc.student_id=s.id " +
                   "join screening_batches b on sc.batch_id=b.id " +
                   "where sc.absent=false and (:schoolId is null or b.school_id=:schoolId) " +
                   "group by grade order by grade", nativeQuery = true)
    List<Object[]> gradeStats(@Param("schoolId") Long schoolId);

    @Query(value = "select count(distinct sc.student_id) from screenings sc " +
                   "join screening_batches b on sc.batch_id=b.id " +
                   "where sc.result='ABNORMAL' and sc.screening_time >= :since", nativeQuery = true)
    long abnormalSince(@Param("since") LocalDateTime since);
}

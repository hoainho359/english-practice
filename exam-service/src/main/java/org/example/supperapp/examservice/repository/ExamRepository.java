package org.example.supperapp.examservice.repository;

import java.util.List;

import org.example.supperapp.examservice.entity.ExamEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ExamRepository extends JpaRepository<ExamEntity, Long> {

    @Query("select distinct e.testNumber from ExamEntity e where e.year = :year order by e.testNumber")
    List<Integer> getListTestOfYear(@Param("year") int year);

    List<ExamEntity> findAllByYearAndTestNumberOrderByPartNumber(int year, int testNumber);
}

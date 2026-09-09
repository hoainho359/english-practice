package org.example.supperapp.examservice.repository;

import java.util.List;

import org.example.supperapp.examservice.entity.Exam;
import org.springframework.data.mongodb.repository.Aggregation;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ExamRepository extends MongoRepository<Exam, String> {

    @Aggregation(
            pipeline = {
                "{ $match: { year: ?0 } }",
                "{ $group: { _id: '$year', testNumbers: { $addToSet: '$testNumber' } } }"
            })
    List<Object> getListTestOfYear(int year);
}

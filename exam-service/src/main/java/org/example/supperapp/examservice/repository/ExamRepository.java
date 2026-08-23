package org.example.supperapp.examservice.repository;

import org.example.supperapp.examservice.entity.Exam;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ExamRepository extends MongoRepository<Exam, String> {
}

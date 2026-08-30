package org.example.supperapp.examservice.service;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.example.supperapp.examservice.repository.ExamRepository;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ExamService {
    ExamRepository examRepository;
    public Object getListTestOfYear(int year){
        return examRepository.getListTestOfYear(year);
    }
}

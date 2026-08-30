package org.example.supperapp.examservice.controller;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.example.supperapp.examservice.dto.response.ApiResponse;
import org.example.supperapp.examservice.service.ExamService;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
@RequestMapping("/exams")
@Slf4j
public class ExamController {
    ExamService examService;
    @GetMapping("/{toeicYear}")
    public ApiResponse<Object> getListTestOfYear(@PathVariable String toeicYear){
        log.info("toeic year:{}", toeicYear);
        int year = Integer.parseInt(StringUtils.split(toeicYear, "-")[1]);
        return ApiResponse.builder()
                .result(examService.getListTestOfYear(year))
                .build();
    }
}

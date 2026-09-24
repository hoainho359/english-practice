package org.example.supperapp.examservice.controller;

import jakarta.validation.Valid;

import org.example.supperapp.examservice.dto.request.ListeningSubmissionRequest;
import org.example.supperapp.examservice.dto.response.ApiResponse;
import org.example.supperapp.examservice.dto.response.ListeningResultResponse;
import org.example.supperapp.examservice.service.ExamService;
import org.example.supperapp.examservice.service.ListeningGradingService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

@RestController
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
@RequestMapping("/exams")
@Slf4j
public class ExamController {
    ExamService examService;
    ListeningGradingService listeningGradingService;

    @GetMapping("/{toeicYear}")
    public ApiResponse<Object> getListTestOfYear(@PathVariable String toeicYear) {
        log.info("toeic year:{}", toeicYear);
        int year = Integer.parseInt(StringUtils.split(toeicYear, "-")[1]);
        return ApiResponse.builder().result(examService.getListTestOfYear(year)).build();
    }

    @PostMapping("/{year}/{testNumber}/listening/submit")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ListeningResultResponse> submitListening(
            @PathVariable int year,
            @PathVariable int testNumber,
            @Valid @RequestBody ListeningSubmissionRequest request,
            Authentication authentication) {
        // FIX: Bind the result to the authenticated JWT subject; never trust a userId sent by the client.
        return ApiResponse.<ListeningResultResponse>builder()
                .message("Listening submission graded")
                .result(listeningGradingService.submit(authentication.getName(), year, testNumber, request))
                .build();
    }
}

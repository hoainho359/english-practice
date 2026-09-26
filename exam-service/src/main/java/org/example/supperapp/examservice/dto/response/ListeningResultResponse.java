package org.example.supperapp.examservice.dto.response;

import java.time.Instant;
import java.util.List;

public record ListeningResultResponse(
        Long resultId,
        String userId,
        Integer year,
        Integer testNumber,
        Integer score,
        Integer totalQuestions,
        Integer answeredQuestions,
        Instant startedAt,
        Instant submittedAt,
        List<GradedListeningAnswerResponse> answers) {}

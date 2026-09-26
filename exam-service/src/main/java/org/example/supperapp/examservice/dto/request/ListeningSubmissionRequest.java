package org.example.supperapp.examservice.dto.request;

import java.time.Instant;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.PastOrPresent;

public record ListeningSubmissionRequest(
        @PastOrPresent Instant startedAt, @NotEmpty List<@Valid ListeningAnswerRequest> answers) {}

package org.example.supperapp.examservice.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record ListeningAnswerRequest(
        @NotNull @Min(1) @Max(100) Integer questionNumber,
        @NotBlank @Pattern(regexp = "(?i)[A-D]", message = "selectedOption must be A, B, C, or D")
                String selectedOption) {}

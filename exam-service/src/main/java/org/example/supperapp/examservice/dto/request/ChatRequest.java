package org.example.supperapp.examservice.dto.request;

import jakarta.validation.constraints.NotBlank;

public record ChatRequest(
        // Reject an empty prompt before spending an external AI API call.
        @NotBlank(message = "message must not be blank") String message) {}

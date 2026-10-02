package org.example.supperapp.examservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Lan gui dau co the bo trong conversationId, cac lan sau dung id server tra ve. */
public record TutorChatRequest(
        String conversationId,
        @Size(max = 80, message = "clientMessageId must not exceed 80 characters") String clientMessageId,
        @NotBlank(message = "message must not be blank") String message) {}

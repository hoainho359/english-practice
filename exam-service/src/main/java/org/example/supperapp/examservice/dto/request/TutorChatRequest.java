package org.example.supperapp.examservice.dto.request;

import jakarta.validation.constraints.NotBlank;

/**
 * A conversation id is optional on the first message. The server creates one and
 * returns it; Expo must send that id with every following message in the same chat.
 */
public record TutorChatRequest(
        String conversationId,
        @NotBlank(message = "message must not be blank") String message) {}

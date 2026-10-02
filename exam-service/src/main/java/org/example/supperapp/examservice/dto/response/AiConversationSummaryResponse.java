package org.example.supperapp.examservice.dto.response;

import java.time.Instant;

public record AiConversationSummaryResponse(
        String id, String title, String lastMessagePreview, long messageCount, Instant lastMessageAt) {}

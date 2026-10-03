package org.example.supperapp.examservice.dto.response;

import java.util.List;

public record AiConversationMessagesResponse(
        String conversationId, String title, List<AiHistoryMessageResponse> messages, String nextCursor) {}

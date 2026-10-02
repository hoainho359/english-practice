package org.example.supperapp.examservice.dto.response;

import java.util.List;

public record AiConversationPageResponse(
        List<AiConversationSummaryResponse> items, int page, int size, long totalElements, boolean hasNext) {}

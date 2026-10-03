package org.example.supperapp.examservice.dto.response;

import java.time.Instant;

import org.example.supperapp.examservice.entity.ai.AiMessageRole;
import org.example.supperapp.examservice.entity.ai.AiMessageStatus;
import org.example.supperapp.examservice.entity.ai.AiMessageType;

public record AiHistoryMessageResponse(
        String id,
        String requestId,
        AiMessageRole role,
        AiMessageType messageType,
        AiMessageStatus status,
        String content,
        TutorChatResponse.LanguageAnalysis analysis,
        QuestionImageExplanationResponse explanation,
        Instant createdAt) {}

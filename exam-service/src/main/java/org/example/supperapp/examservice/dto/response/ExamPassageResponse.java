package org.example.supperapp.examservice.dto.response;

import org.example.supperapp.examservice.entity.enumeric.ContextType;

public record ExamPassageResponse(
        Long id,
        Integer groupNumber,
        Integer partNumber,
        Integer firstQuestionNumber,
        Integer lastQuestionNumber,
        ContextType contextType,
        String content,
        String audioUrl) {}

package org.example.supperapp.examservice.dto.response;

import java.util.List;

public record ExamQuestionResponse(
        Long id,
        Integer questionNumber,
        String content,
        String imageUrl,
        String imageBase64,
        Long passageId,
        List<ExamOptionResponse> options) {}

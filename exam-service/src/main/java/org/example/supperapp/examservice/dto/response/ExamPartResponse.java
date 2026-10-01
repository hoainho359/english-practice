package org.example.supperapp.examservice.dto.response;

import java.util.List;

import org.example.supperapp.examservice.entity.enumeric.PartType;

public record ExamPartResponse(
        Long examId,
        Integer year,
        Integer testNumber,
        Integer partNumber,
        PartType type,
        String title,
        List<ExamPassageResponse> passages,
        List<ExamQuestionResponse> questions) {}

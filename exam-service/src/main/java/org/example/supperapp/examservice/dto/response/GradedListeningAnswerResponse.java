package org.example.supperapp.examservice.dto.response;

public record GradedListeningAnswerResponse(Integer questionNumber, String selectedOption, Boolean correct) {}

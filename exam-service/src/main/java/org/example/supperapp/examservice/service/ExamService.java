package org.example.supperapp.examservice.service;

import java.util.Comparator;

import org.example.supperapp.examservice.dto.response.ExamOptionResponse;
import org.example.supperapp.examservice.dto.response.ExamPartResponse;
import org.example.supperapp.examservice.dto.response.ExamPassageResponse;
import org.example.supperapp.examservice.dto.response.ExamQuestionResponse;
import org.example.supperapp.examservice.entity.ExamEntity;
import org.example.supperapp.examservice.entity.OptionEntity;
import org.example.supperapp.examservice.entity.PassageEntity;
import org.example.supperapp.examservice.entity.QuestionEntity;
import org.example.supperapp.examservice.entity.enumeric.ContextType;
import org.example.supperapp.examservice.exception.AppException;
import org.example.supperapp.examservice.exception.ErrorCode;
import org.example.supperapp.examservice.repository.ExamRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ExamService {
    ExamRepository examRepository;

    public Object getListTestOfYear(int year) {
        return examRepository.getListTestOfYear(year);
    }

    @Transactional(readOnly = true)
    public ExamPartResponse getPart(int year, int testNumber, int partNumber) {
        if (partNumber < 1 || partNumber > 7) {
            throw new AppException(ErrorCode.INVALID_EXAM_PART);
        }

        ExamEntity exam = examRepository
                .findByYearAndTestNumberAndPartNumber(year, testNumber, partNumber)
                .orElseThrow(() -> new AppException(ErrorCode.EXAM_PART_NOT_FOUND));

        var passages = exam.getPassages().stream()
                .sorted(Comparator.comparing(PassageEntity::getGroupNumber))
                .map(this::toPassageResponse)
                .toList();

        var questions = exam.getQuestions().stream()
                .sorted(Comparator.comparing(QuestionEntity::getQuestionNumber))
                .map(this::toQuestionResponse)
                .toList();

        return new ExamPartResponse(
                exam.getId(),
                exam.getYear(),
                exam.getTestNumber(),
                exam.getPartNumber(),
                exam.getType(),
                exam.getTitle(),
                passages,
                questions);
    }

    private ExamPassageResponse toPassageResponse(PassageEntity passage) {
        // Listening content là transcript/đáp án nghe, chỉ được trả ở API review sau khi nộp bài.
        String visibleContent = passage.getContextType() == ContextType.READING_PASSAGE
                ? passage.getContent()
                : null;

        return new ExamPassageResponse(
                passage.getId(),
                passage.getGroupNumber(),
                passage.getPartNumber(),
                passage.getFirstQuestionNumber(),
                passage.getLastQuestionNumber(),
                passage.getContextType(),
                visibleContent,
                passage.getAudioUrl());
    }

    private ExamQuestionResponse toQuestionResponse(QuestionEntity question) {
        var options = question.getOptions().stream()
                .sorted(Comparator.comparing(OptionEntity::getLabel))
                .map(option -> new ExamOptionResponse(option.getId(), option.getLabel(), option.getText()))
                .toList();

        Long passageId = question.getPassage() == null ? null : question.getPassage().getId();

        // Không map correctOption và explanation để tránh lộ đáp án khi đang làm bài.
        return new ExamQuestionResponse(
                question.getId(),
                question.getQuestionNumber(),
                question.getContent(),
                question.getImageUrl(),
                question.getImageBase64(),
                passageId,
                options);
    }
}

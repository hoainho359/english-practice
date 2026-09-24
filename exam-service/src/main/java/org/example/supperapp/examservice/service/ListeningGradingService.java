package org.example.supperapp.examservice.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.example.supperapp.examservice.dto.request.ListeningAnswerRequest;
import org.example.supperapp.examservice.dto.request.ListeningSubmissionRequest;
import org.example.supperapp.examservice.dto.response.GradedListeningAnswerResponse;
import org.example.supperapp.examservice.dto.response.ListeningResultResponse;
import org.example.supperapp.examservice.entity.ExamEntity;
import org.example.supperapp.examservice.entity.OptionEntity;
import org.example.supperapp.examservice.entity.QuestionEntity;
import org.example.supperapp.examservice.entity.Result;
import org.example.supperapp.examservice.entity.UserAnswer;
import org.example.supperapp.examservice.exception.ExamSubmissionException;
import org.example.supperapp.examservice.repository.ExamRepository;
import org.example.supperapp.examservice.repository.ResultRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ListeningGradingService {

    private static final int FIRST_LISTENING_PART = 1;
    private static final int LAST_LISTENING_PART = 4;
    private static final int LISTENING_QUESTION_COUNT = 100;

    private final ExamRepository examRepository;
    private final ResultRepository resultRepository;

    @Transactional
    public ListeningResultResponse submit(
            String userId, int year, int testNumber, ListeningSubmissionRequest submission) {
        if (userId == null || userId.isBlank()) {
            throw error(3000, "Authenticated user is missing", HttpStatus.UNAUTHORIZED);
        }

        List<ExamEntity> listeningParts =
                examRepository.findAllByYearAndTestNumberOrderByPartNumber(year, testNumber).stream()
                        .filter(part -> part.getPartNumber() >= FIRST_LISTENING_PART
                                && part.getPartNumber() <= LAST_LISTENING_PART)
                        .toList();
        Set<Integer> partNumbers =
                listeningParts.stream().map(ExamEntity::getPartNumber).collect(Collectors.toSet());
        if (!partNumbers.equals(Set.of(1, 2, 3, 4))) {
            throw error(3001, "Listening test was not found or does not contain Parts 1-4", HttpStatus.NOT_FOUND);
        }

        Map<Integer, QuestionEntity> questions = collectQuestions(listeningParts);
        if (questions.size() != LISTENING_QUESTION_COUNT) {
            throw error(
                    3002,
                    "Listening test contains " + questions.size() + " questions; expected 100",
                    HttpStatus.CONFLICT);
        }
        if (questions.values().stream()
                .anyMatch(question -> question.getCorrectOption() == null
                        || question.getCorrectOption().isBlank())) {
            throw error(3003, "Listening answers have not been imported completely", HttpStatus.CONFLICT);
        }

        Map<Integer, ListeningAnswerRequest> submittedAnswers = new LinkedHashMap<>();
        for (ListeningAnswerRequest answer : submission.answers()) {
            if (submittedAnswers.putIfAbsent(answer.questionNumber(), answer) != null) {
                throw error(
                        3004,
                        "Question " + answer.questionNumber() + " was submitted more than once",
                        HttpStatus.BAD_REQUEST);
            }
        }

        List<UserAnswer> gradedAnswers = new ArrayList<>();
        int score = 0;
        for (ListeningAnswerRequest submittedAnswer : submittedAnswers.values()) {
            QuestionEntity question = questions.get(submittedAnswer.questionNumber());
            if (question == null) {
                throw error(
                        3005,
                        "Question " + submittedAnswer.questionNumber() + " is not part of this Listening test",
                        HttpStatus.BAD_REQUEST);
            }

            String selectedOption = normalizeOption(submittedAnswer.selectedOption());
            Set<String> validOptions = question.getOptions().stream()
                    .map(OptionEntity::getLabel)
                    .map(this::normalizeOption)
                    .collect(Collectors.toSet());
            if (!validOptions.contains(selectedOption)) {
                throw error(
                        3006,
                        "Option " + selectedOption + " does not exist for question " + question.getQuestionNumber(),
                        HttpStatus.BAD_REQUEST);
            }

            // FIX: Grading is deterministic; AI is never used to decide whether an answer is correct.
            boolean correct = selectedOption.equals(normalizeOption(question.getCorrectOption()));
            if (correct) {
                score++;
            }
            gradedAnswers.add(new UserAnswer(question.getQuestionNumber(), selectedOption, correct));
        }

        Instant submittedAt = Instant.now();
        Instant startedAt = submission.startedAt() == null ? submittedAt : submission.startedAt();
        Result result = Result.builder()
                .userId(userId)
                .examYear(year)
                .testNumber(testNumber)
                .startedAt(startedAt)
                .submittedAt(submittedAt)
                .score(score)
                .totalQuestions(LISTENING_QUESTION_COUNT)
                .answers(gradedAnswers)
                .build();

        // FIX: exam_results/user_answers are created only for an actual user submission, never during Admin upload.
        Result saved = resultRepository.saveAndFlush(result);
        return toResponse(saved);
    }

    private Map<Integer, QuestionEntity> collectQuestions(List<ExamEntity> parts) {
        return parts.stream()
                .flatMap(part -> part.getQuestions().stream())
                .collect(Collectors.toMap(
                        QuestionEntity::getQuestionNumber,
                        Function.identity(),
                        (first, duplicate) -> first,
                        LinkedHashMap::new));
    }

    private String normalizeOption(String option) {
        return option.trim().toUpperCase(Locale.ROOT);
    }

    private ListeningResultResponse toResponse(Result result) {
        List<GradedListeningAnswerResponse> answers = result.getAnswers().stream()
                .map(answer -> new GradedListeningAnswerResponse(
                        answer.getQuestionNumber(), answer.getSelectedOption(), answer.getCorrect()))
                .toList();
        return new ListeningResultResponse(
                result.getId(),
                result.getUserId(),
                result.getExamYear(),
                result.getTestNumber(),
                result.getScore(),
                result.getTotalQuestions(),
                answers.size(),
                result.getStartedAt(),
                result.getSubmittedAt(),
                answers);
    }

    private ExamSubmissionException error(int code, String message, HttpStatus status) {
        return new ExamSubmissionException(code, message, status);
    }
}

package org.example.supperapp.examservice.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.example.supperapp.examservice.entity.ExamEntity;
import org.example.supperapp.examservice.entity.OptionEntity;
import org.example.supperapp.examservice.entity.PassageEntity;
import org.example.supperapp.examservice.entity.QuestionEntity;
import org.example.supperapp.examservice.entity.enumeric.ContextType;
import org.example.supperapp.examservice.entity.enumeric.PartType;
import org.example.supperapp.examservice.exception.AppException;
import org.example.supperapp.examservice.exception.ErrorCode;
import org.example.supperapp.examservice.repository.ExamRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ExamServiceTest {

    @Mock
    ExamRepository examRepository;

    @InjectMocks
    ExamService examService;

    @Test
    void getPartReturnsReadingContextAndSortedQuestions() {
        ExamEntity exam = createExam(7, PartType.READING, ContextType.READING_PASSAGE, "Reading passage");
        when(examRepository.findByYearAndTestNumberAndPartNumber(2026, 1, 7)).thenReturn(Optional.of(exam));

        var response = examService.getPart(2026, 1, 7);

        assertThat(response.partNumber()).isEqualTo(7);
        assertThat(response.passages()).singleElement().extracting("content").isEqualTo("Reading passage");
        assertThat(response.questions()).extracting(question -> question.questionNumber()).containsExactly(147, 148);
        assertThat(response.questions().getFirst().options())
                .extracting(option -> option.label())
                .containsExactly("A", "B");
    }

    @Test
    void getPartHidesListeningTranscript() {
        ExamEntity exam = createExam(3, PartType.LISTENING, ContextType.LISTENING_TRANSCRIPT, "Secret transcript");
        when(examRepository.findByYearAndTestNumberAndPartNumber(2026, 1, 3)).thenReturn(Optional.of(exam));

        var response = examService.getPart(2026, 1, 3);

        assertThat(response.passages()).singleElement().extracting("content").isNull();
    }

    @Test
    void getPartRejectsPartOutsideToeicRange() {
        assertThatThrownBy(() -> examService.getPart(2026, 1, 8))
                .isInstanceOf(AppException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_EXAM_PART);
    }

    private ExamEntity createExam(
            int partNumber, PartType partType, ContextType contextType, String passageContent) {
        OptionEntity optionB = OptionEntity.builder().label("B").text("Second option").build();
        optionB.setId(2L);
        OptionEntity optionA = OptionEntity.builder().label("A").text("First option").build();
        optionA.setId(1L);

        QuestionEntity question148 = QuestionEntity.builder()
                .questionNumber(148)
                .content("Question 148")
                .correctOption("B")
                .explanation("Hidden explanation")
                .options(new ArrayList<>(List.of(optionB, optionA)))
                .build();
        question148.setId(148L);
        question148.wireOptions();

        QuestionEntity question147 = QuestionEntity.builder()
                .questionNumber(147)
                .content("Question 147")
                .correctOption("A")
                .explanation("Hidden explanation")
                .options(new ArrayList<>(List.of(optionB, optionA)))
                .build();
        question147.setId(147L);
        question147.wireOptions();

        PassageEntity passage = PassageEntity.builder()
                .groupNumber(1)
                .partNumber(partNumber)
                .firstQuestionNumber(147)
                .lastQuestionNumber(148)
                .contextType(contextType)
                .content(passageContent)
                .audioUrl("https://example.test/audio.mp3")
                .questions(new ArrayList<>(List.of(question148, question147)))
                .build();
        passage.setId(10L);
        question147.setPassage(passage);
        question148.setPassage(passage);

        ExamEntity exam = ExamEntity.builder()
                .year(2026)
                .testNumber(1)
                .partNumber(partNumber)
                .type(partType)
                .title("Part " + partNumber)
                .questions(new ArrayList<>(List.of(question148, question147)))
                .passages(new ArrayList<>(List.of(passage)))
                .build();
        exam.setId(100L);
        return exam;
    }
}

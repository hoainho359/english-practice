package org.example.supperapp.examservice.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.example.supperapp.examservice.entity.ExamEntity;
import org.example.supperapp.examservice.entity.QuestionEntity;
import org.example.supperapp.examservice.repository.ExamRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class ListeningAnswerImportIntegrationTest {

    @Autowired
    private FileService fileService;

    @Autowired
    private ExamRepository examRepository;

    @Test
    void importsAnswerKeyAndMapsTranscriptsToListeningQuestions() throws IOException {
        String testBookPath = System.getProperty("toeic.listening.pdf");
        String answerBookPath = System.getProperty("toeic.listening.answer.pdf");
        assumeTrue(testBookPath != null && answerBookPath != null);

        fileService.uploadFile(pdf("listening.pdf", testBookPath), 2026, 1);
        fileService.uploadListeningAnswer(pdf("listening-answer.pdf", answerBookPath), 2026, 1);
        examRepository.flush();

        var parts = examRepository.findAllByYearAndTestNumberOrderByPartNumber(2026, 1);
        assertThat(parts).hasSize(4);

        ExamEntity part1 = parts.getFirst();
        QuestionEntity question1 = question(part1, 1);
        assertThat(question1.getCorrectOption()).isEqualTo("B");
        assertThat(question1.getPassage().getContent())
                .contains("The woman is carrying a tray of food", "The woman is wearing a jacket");

        ExamEntity part2 = parts.get(1);
        QuestionEntity question7 = question(part2, 7);
        assertThat(question7.getCorrectOption()).isEqualTo("B");
        assertThat(question7.getPassage().getContent())
                .contains("Where is the conference being held", "At the Riverview Hotel");

        ExamEntity part3 = parts.get(2);
        QuestionEntity question32 = question(part3, 32);
        assertThat(question32.getPassage()).isSameAs(question(part3, 34).getPassage());
        assertThat(question32.getPassage().getContent()).contains("focus group results", "production manager");

        ExamEntity part4 = parts.get(3);
        assertThat(question(part4, 100).getCorrectOption()).isEqualTo("A");
        assertThat(question(part4, 100).getPassage().getContent())
                .contains("prospective investors", "necessary permits");
    }

    private QuestionEntity question(ExamEntity exam, int questionNumber) {
        return exam.getQuestions().stream()
                .filter(question -> question.getQuestionNumber() == questionNumber)
                .findFirst()
                .orElseThrow();
    }

    private MockMultipartFile pdf(String name, String path) throws IOException {
        return new MockMultipartFile(name, name, "application/pdf", Files.readAllBytes(Path.of(path)));
    }
}

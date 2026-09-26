package org.example.supperapp.examservice.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Comparator;

import org.example.supperapp.examservice.dto.request.ListeningAnswerRequest;
import org.example.supperapp.examservice.dto.request.ListeningSubmissionRequest;
import org.example.supperapp.examservice.entity.ExamEntity;
import org.example.supperapp.examservice.entity.QuestionEntity;
import org.example.supperapp.examservice.exception.PdfImportException;
import org.example.supperapp.examservice.repository.ExamRepository;
import org.example.supperapp.examservice.repository.ResultRepository;
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

    @Autowired
    private ListeningGradingService listeningGradingService;

    @Autowired
    private ResultRepository resultRepository;

    @Test
    void rejectsAFileWhoseContentIsNotPdf() {
        MockMultipartFile fakePdf =
                new MockMultipartFile("file", "answer.pdf", "application/pdf", "not-a-pdf".getBytes());

        assertThatThrownBy(() -> fileService.uploadListeningAnswer(fakePdf, 2026, 1))
                .isInstanceOf(PdfImportException.class)
                .hasMessage("Uploaded file must be a valid PDF document");
    }

    @Test
    void importsAnswerKeyAndMapsTranscriptsToListeningQuestions() throws IOException {
        String testBookPath = System.getProperty("toeic.listening.pdf");
        String answerBookPath = System.getProperty("toeic.listening.answer.pdf");
        assumeTrue(testBookPath != null && answerBookPath != null);

        fileService.uploadFile(pdf("listening.pdf", testBookPath), 2026, 1);
        fileService.uploadListeningAnswer(pdf("listening-answer.pdf", answerBookPath), 2026, 1);
        examRepository.flush();

        var parts = examRepository.findAllByYearAndTestNumberOrderByPartNumber(2026, 1);
        // The fixture may be Listening-only (4 parts) or a merged TOEIC book (7 parts).
        // The transcript import contract only requires Listening Parts 1-4 to exist.
        assertThat(parts).extracting(ExamEntity::getPartNumber).contains(1, 2, 3, 4);

        ExamEntity part1 = part(parts, 1);
        QuestionEntity question1 = question(part1, 1);
        assertThat(question1.getCorrectOption()).isEqualTo("B");
        assertThat(question1.getPassage().getContent())
                .contains("The woman is carrying a tray of food", "The woman is wearing a jacket");

        ExamEntity part2 = part(parts, 2);
        QuestionEntity question7 = question(part2, 7);
        assertThat(question7.getCorrectOption()).isEqualTo("B");
        assertThat(question7.getPassage().getContent())
                .contains("Where is the conference being held", "At the Riverview Hotel");

        ExamEntity part3 = part(parts, 3);
        QuestionEntity question32 = question(part3, 32);
        assertThat(question32.getPassage()).isSameAs(question(part3, 34).getPassage());
        assertThat(question32.getPassage().getContent()).contains("focus group results", "production manager");

        ExamEntity part4 = part(parts, 4);
        assertThat(question(part4, 100).getCorrectOption()).isEqualTo("A");
        assertThat(question(part4, 100).getPassage().getContent())
                .contains("prospective investors", "necessary permits");

        var submittedAnswers = parts.stream()
                .filter(part -> part.getPartNumber() <= 4)
                .flatMap(part -> part.getQuestions().stream())
                .sorted(Comparator.comparing(QuestionEntity::getQuestionNumber))
                .map(question -> new ListeningAnswerRequest(
                        question.getQuestionNumber(),
                        // Submit one deliberately wrong answer to verify answer comparison and score persistence.
                        question.getQuestionNumber() == 1 ? "A" : question.getCorrectOption()))
                .toList();
        var submission = new ListeningSubmissionRequest(Instant.now().minusSeconds(600), submittedAnswers);

        var gradedResult = listeningGradingService.submit("integration-user", 2026, 1, submission);

        assertThat(gradedResult.score()).isEqualTo(99);
        assertThat(gradedResult.totalQuestions()).isEqualTo(100);
        assertThat(gradedResult.answeredQuestions()).isEqualTo(100);
        var persistedResult = resultRepository.findById(gradedResult.resultId()).orElseThrow();
        assertThat(persistedResult.getAnswers()).hasSize(100);
        assertThat(persistedResult.getAnswers().getFirst().getCorrect()).isFalse();
    }

    private ExamEntity part(java.util.List<ExamEntity> parts, int partNumber) {
        return parts.stream()
                .filter(part -> part.getPartNumber() == partNumber)
                .findFirst()
                .orElseThrow();
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

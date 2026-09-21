package org.example.supperapp.examservice.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.example.supperapp.examservice.entity.ExamEntity;
import org.example.supperapp.examservice.entity.QuestionEntity;
import org.example.supperapp.examservice.exception.PdfImportException;
import org.junit.jupiter.api.Test;

class ToeicPdfParserTest {

    private final ToeicPdfParser parser = new ToeicPdfParser(null);

    @Test
    void usesTheAnswerBlockInsteadOfThePart6BlankMarker() {
        String extracted =
                """
				131.
				Passage text around the first blank.
				132.
				More passage text.
				131. (A) first answer
				(B) second answer
				(C) third answer
				(D) fourth answer
				132. (A) another answer
				(B) another answer
				(C) another answer
				(D) another answer
				""";

        QuestionEntity question = parser.parseQuestionByNumber(extracted, 131);

        assertThat(question).isNotNull();
        assertThat(question.getOptions())
                .extracting(option -> option.getText())
                .containsExactly("first answer", "second answer", "third answer", "fourth answer");
    }

    @Test
    void rejectsAQuestionWithAMissingOption() {
        String extracted =
                """
				101. Which word completes the sentence?
				(A) first
				(B) second
				(C) third
				""";

        assertThatThrownBy(() -> parser.parseQuestions(extracted, 101, 101))
                .isInstanceOf(PdfImportException.class)
                .hasMessageContaining("Question 101")
                .hasMessageContaining("options A-D");
    }

    @Test
    void findsReadingGroupHeadersWithDifferentDashCharacters() {
        String extracted =
                """
				Questions 131-134 refer to the following flyer.
				First passage
				Questions 135–138 refer to the following letter.
				Second passage
				""";

        List<ToeicPdfParser.GroupHeader> groups = parser.findGroupHeaders(extracted, 131, 138);

        assertThat(groups).extracting(ToeicPdfParser.GroupHeader::firstQuestion).containsExactly(131, 135);
        assertThat(groups).extracting(ToeicPdfParser.GroupHeader::lastQuestion).containsExactly(134, 138);
    }

    @Test
    void parsesTheProvidedEtsFixturesWhenPathsAreSupplied() throws IOException {
        String listeningPath = System.getProperty("toeic.listening.pdf");
        String readingPath = System.getProperty("toeic.reading.pdf");
        assumeTrue(listeningPath != null && readingPath != null);

        try (PDDocument listening = Loader.loadPDF(new File(listeningPath));
                PDDocument reading = Loader.loadPDF(new File(readingPath))) {
            List<ExamEntity> listeningParts = parser.parse(listening, 2026, 1);
            List<ExamEntity> readingParts = parser.parse(reading, 2026, 1);

            assertThat(listeningParts).extracting(ExamEntity::getPartNumber).containsExactly(1, 2, 3, 4);
            assertThat(listeningParts.get(0).getQuestions()).hasSize(6);
            assertThat(listeningParts.get(1).getQuestions()).hasSize(25);
            assertThat(totalGroupedQuestions(listeningParts.get(2))).isEqualTo(39);
            assertThat(totalGroupedQuestions(listeningParts.get(3))).isEqualTo(30);
            assertThat(listeningParts
                            .get(2)
                            .getPassages()
                            .getFirst()
                            .getQuestions()
                            .getFirst()
                            .getContent())
                    .isEqualTo("What type of food product does the speakers’ company sell?");
            assertThat(listeningParts
                            .get(3)
                            .getPassages()
                            .getLast()
                            .getQuestions()
                            .getLast()
                            .getOptions())
                    .extracting(option -> option.getText())
                    .containsExactly(
                            "Applying for permits",
                            "Installing equipment",
                            "Hiring additional staff",
                            "Updating a manual");
            assertThat(listeningParts.stream()
                            .flatMap(part -> part.getPassages().stream())
                            .flatMap(group -> group.getQuestions().stream())
                            .filter(question -> question.getContent().contains("graphic")))
                    .isNotEmpty()
                    .allSatisfy(
                            question -> assertThat(question.getImageBase64()).isNotBlank());

            assertThat(readingParts).extracting(ExamEntity::getPartNumber).containsExactly(5, 6, 7);
            assertThat(readingParts.get(0).getQuestions()).hasSize(30);
            assertThat(totalGroupedQuestions(readingParts.get(1))).isEqualTo(16);
            assertThat(totalGroupedQuestions(readingParts.get(2))).isEqualTo(54);
            assertThat(readingParts.get(1).getPassages())
                    .allSatisfy(group -> assertThat(group.getContent()).isNotBlank());
            assertThat(readingParts.get(1).getPassages().getFirst().getContent())
                    .contains("Riessler Landscaping", "[131]", "[134]");

            listeningParts.subList(2, 4).forEach(this::assertCompleteGroupedQuestions);
            assertCompleteQuestions(readingParts.get(0).getQuestions());
            assertCompleteGroupedQuestions(readingParts.get(1));
            assertCompleteGroupedQuestions(readingParts.get(2));
        }
    }

    private void assertCompleteGroupedQuestions(ExamEntity exam) {
        exam.getPassages().forEach(group -> assertCompleteQuestions(group.getQuestions()));
    }

    private void assertCompleteQuestions(List<QuestionEntity> questions) {
        questions.forEach(question -> {
            assertThat(question.getContent()).isNotBlank();
            assertThat(question.getOptions()).hasSize(4).allSatisfy(option -> {
                assertThat(option.getText()).isNotBlank();
                assertThat(option.getText()).doesNotContain("GO ON TO THE NEXT PAGE", "Questions ");
            });
        });
    }

    private int totalGroupedQuestions(ExamEntity exam) {
        return exam.getPassages().stream()
                .mapToInt(group -> group.getQuestions().size())
                .sum();
    }
}

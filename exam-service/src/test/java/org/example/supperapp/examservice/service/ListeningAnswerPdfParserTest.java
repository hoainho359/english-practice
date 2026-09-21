package org.example.supperapp.examservice.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.File;
import java.io.IOException;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.junit.jupiter.api.Test;

class ListeningAnswerPdfParserTest {

    private final ListeningAnswerPdfParser parser = new ListeningAnswerPdfParser();

    @Test
    void normalizesBrokenCCharactersInTheAnswerKey() {
        var answers = parser.parseAnswerKey("1(B) 2(D) 3(0) 4(A) 5(O)");

        assertThat(answers).containsEntry(1, "B").containsEntry(3, "C").containsEntry(5, "C");
    }

    @Test
    void parsesSingleQuestionTranscriptsAndOptions() {
        String extracted =
                """
				1 W-Br
				(A) First description.
				(B) Second description.
				(C) Third description.
				(D) Fourth description.
				(A) translated content
				7 W-Am / M-Au
				Where is the conference being held?
				(A) A three-day vacation.
				(B) At the Riverview Hotel.
				(C) In the supply cabinet.
				(A) translated content
				""";

        var questions = parser.parseSpokenQuestions(extracted);

        assertThat(questions.get(1).options()).hasSize(4);
        assertThat(questions.get(7).spokenPrompt()).isEqualTo("Where is the conference being held?");
        assertThat(questions.get(7).options().get("B")).isEqualTo("At the Riverview Hotel.");
    }

    @Test
    void parsesTheProvidedListeningAnswerFixture() throws IOException {
        String path = System.getProperty("toeic.listening.answer.pdf");
        assumeTrue(path != null);

        try (PDDocument document = Loader.loadPDF(new File(path))) {
            var result = parser.parse(document);

            assertThat(result.answerKey()).hasSize(100);
            assertThat(result.answerKey()).containsEntry(1, "B").containsEntry(100, "A");
            assertThat(result.spokenQuestions()).hasSize(31);
            assertThat(result.spokenQuestions().get(7).spokenPrompt()).contains("Where is the conference being held");
            assertThat(result.groupTranscripts()).hasSize(23);
            assertThat(result.groupTranscripts().getFirst().firstQuestion()).isEqualTo(32);
            assertThat(result.groupTranscripts().getFirst().transcript())
                    .contains("focus group results", "spicy Cheddar cheese");
            assertThat(result.groupTranscripts().getLast().lastQuestion()).isEqualTo(100);
            assertThat(result.groupTranscripts().getLast().transcript())
                    .contains("prospective investors", "necessary permits");
        }
    }
}

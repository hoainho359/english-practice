package org.example.supperapp.examservice.dto.response;

import java.time.Instant;
import java.util.List;

/** Du lieu co cau truc de Expo hien thi nam phan giai thich TOEIC. */
public record QuestionImageExplanationResponse(
        String conversationId,
        String requestId,
        String messageId,
        String responseType,
        Instant createdAt,
        RecognizedQuestion question,
        CorrectAnswer correctAnswer,
        Translation translation,
        GrammarAnalysis grammarAnalysis,
        List<OptionAnalysis> optionAnalysis,
        List<VocabularyItem> vocabulary,
        double confidence,
        List<String> warnings) {

    public record RecognizedQuestion(String content, List<RecognizedOption> options) {}

    public record RecognizedOption(String label, String text) {}

    public record CorrectAnswer(String label, String text) {}

    public record Translation(String completedSentence, String vietnameseMeaning) {}

    public record GrammarAnalysis(String grammarPoint, String structure, String explanation) {}

    public record OptionAnalysis(String label, String text, boolean correct, String reason) {}

    public record VocabularyItem(String word, String partOfSpeech, String meaning, String note) {}
}

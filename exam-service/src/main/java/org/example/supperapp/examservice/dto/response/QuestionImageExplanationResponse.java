package org.example.supperapp.examservice.dto.response;

import java.util.List;

/** Structured result used by Expo to render the five TOEIC explanation sections. */
public record QuestionImageExplanationResponse(
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

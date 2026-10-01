package org.example.supperapp.examservice.dto.response;

import java.util.List;

/** Structured conversational response using Record java17*/
public record TutorChatResponse(
        String conversationId,
        LanguageAnalysis analysis,
        String reply,
        List<String> followUpSuggestions) {

    public record LanguageAnalysis(
            String detectedLanguage,
            String originalMessage,
            String correctedMessage,
            List<GrammarIssue> grammarIssues,
            List<VocabularyFeedback> vocabulary) {}

    public record GrammarIssue(
            String original,
            String correction,
            String grammarRule,
            String explanation) {}

    public record VocabularyFeedback(
            String wordOrPhrase,
            String partOfSpeech,
            String vietnameseMeaning,
            String usageNote,
            String example) {}
}

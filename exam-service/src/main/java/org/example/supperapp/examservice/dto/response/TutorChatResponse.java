package org.example.supperapp.examservice.dto.response;

import java.time.Instant;
import java.util.List;

/** Du lieu chat co cau truc de Expo render on dinh. */
public record TutorChatResponse(
        String conversationId,
        String requestId,
        String messageId,
        String responseType,
        LanguageAnalysis analysis,
        String reply,
        List<String> followUpSuggestions,
        Instant createdAt) {

    public record LanguageAnalysis(
            String detectedLanguage,
            String originalMessage,
            String correctedMessage,
            List<GrammarIssue> grammarIssues,
            List<VocabularyFeedback> vocabulary) {}

    public record GrammarIssue(String original, String correction, String grammarRule, String explanation) {}

    public record VocabularyFeedback(
            String wordOrPhrase, String partOfSpeech, String vietnameseMeaning, String usageNote, String example) {}
}

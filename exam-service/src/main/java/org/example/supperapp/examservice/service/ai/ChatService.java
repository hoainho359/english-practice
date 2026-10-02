package org.example.supperapp.examservice.service.ai;

import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.example.supperapp.examservice.dto.request.TutorChatRequest;
import org.example.supperapp.examservice.dto.response.QuestionImageExplanationResponse;
import org.example.supperapp.examservice.dto.response.TutorChatResponse;
import org.example.supperapp.examservice.entity.ai.AiConversationEntity;
import org.example.supperapp.examservice.entity.ai.AiMessageType;
import org.example.supperapp.examservice.exception.AppException;
import org.example.supperapp.examservice.exception.ErrorCode;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.content.Media;
import org.springframework.ai.retry.NonTransientAiException;
import org.springframework.ai.retry.TransientAiException;
import org.springframework.stereotype.Service;
import org.springframework.util.MimeType;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ChatService {

    private static final long MAX_IMAGE_SIZE = 10 * 1024 * 1024;
    private static final Set<String> SUPPORTED_IMAGE_TYPES = Set.of("image/jpeg", "image/png", "image/webp");

    private static final String IMAGE_EXPLANATION_SYSTEM_PROMPT =
            """
			You are a senior TOEIC teacher and an OCR-aware question solver.
			Read the question and all answer options from the uploaded image.
			Return the result in the requested schema and use Vietnamese for explanations.

			Requirements:
			1. Select exactly one correct answer when the image contains enough information.
			2. Provide the completed English sentence and its natural Vietnamese translation.
			3. Explain the grammar point, grammar structure, and why the answer is correct.
			4. Analyze every visible option and explain why it is correct or incorrect.
			5. Extract only useful TOEIC vocabulary visible in or directly related to the question.
			6. Never invent unreadable text. Put OCR uncertainty in warnings and lower confidence.
			7. confidence must be between 0 and 1.
			8. Return data only for the requested schema. Do not wrap it in Markdown.
			""";

    private static final String CONVERSATION_SYSTEM_PROMPT =
            """
			You are Tuyen, a friendly AI English and TOEIC tutor.
			Continue the conversation naturally using the previous messages.
			For every user message, return the requested structured schema.

			Language feedback rules:
			- Preserve the user's intended meaning.
			- If the user writes English, identify concrete grammar mistakes and provide corrections.
			- If there is no grammar mistake, return an empty grammarIssues list.
			- Extract useful vocabulary or phrases, with Vietnamese meanings and usage examples.
			- reply must answer the user's actual question, not only correct their English.
			- reply should be encouraging, concise, and invite a useful next turn.
			- Never claim an error exists when the original sentence is already correct.
			- Return data only for the requested schema. Do not wrap it in Markdown.
			""";

    private final ChatClient chatClient;
    private final ChatHistoryService historyService;

    public ChatService(ChatClient.Builder builder, ChatHistoryService historyService) {
        this.chatClient = builder.build();
        this.historyService = historyService;
    }

    public TutorChatResponse chat(String userId, TutorChatRequest request) {
        String requestId = requestId(request.clientMessageId());
        String conversationId = existingConversationId(userId, requestId, request.conversationId());
        AiConversationEntity conversation =
                historyService.resolveConversation(userId, conversationId, request.message());
        var cached = historyService.findTutorResponse(conversation.getId(), requestId);
        if (cached.isPresent()) return cached.get();

        var context = historyService.loadContext(conversation.getId(), requestId);
        historyService.saveUserMessage(
                userId,
                conversation.getId(),
                requestId,
                AiMessageType.TEXT,
                request.message().trim());

        TutorChatModelResult generated;
        try {
            generated = chatClient
                    .prompt()
                    .system(CONVERSATION_SYSTEM_PROMPT)
                    .messages(context)
                    .user(request.message())
                    .call()
                    .entity(TutorChatModelResult.class);
        } catch (TransientAiException | NonTransientAiException exception) {
            historyService.saveFailedAssistant(userId, conversation.getId(), requestId, AiMessageType.TUTOR_REPLY);
            throw exception;
        } catch (RuntimeException exception) {
            historyService.saveFailedAssistant(userId, conversation.getId(), requestId, AiMessageType.TUTOR_REPLY);
            throw new AppException(ErrorCode.INVALID_AI_RESPONSE);
        }

        if (generated == null || !StringUtils.hasText(generated.reply())) {
            historyService.saveFailedAssistant(userId, conversation.getId(), requestId, AiMessageType.TUTOR_REPLY);
            throw new AppException(ErrorCode.INVALID_AI_RESPONSE);
        }

        TutorChatResponse response = new TutorChatResponse(
                conversation.getId().toString(),
                requestId,
                UUID.randomUUID().toString(),
                "TUTOR_REPLY",
                normalizeAnalysis(generated.analysis(), request.message()),
                generated.reply().trim(),
                cleanStrings(generated.followUpSuggestions()),
                Instant.now());
        historyService.saveAssistantMessage(
                userId, conversation.getId(), requestId, AiMessageType.TUTOR_REPLY, response.reply(), response);
        return response;
    }

    public QuestionImageExplanationResponse explainImage(
            String userId, MultipartFile file, String instruction, String conversationId, String clientMessageId) {
        MimeType mimeType = validateImage(file);
        String userInstruction = StringUtils.hasText(instruction)
                ? instruction.trim()
                : "Giải thích câu hỏi trong ảnh này theo năm phần.";
        String requestId = requestId(clientMessageId);
        conversationId = existingConversationId(userId, requestId, conversationId);
        AiConversationEntity conversation =
                historyService.resolveConversation(userId, conversationId, "Giải thích ảnh câu hỏi TOEIC");
        var cached = historyService.findImageResponse(conversation.getId(), requestId);
        if (cached.isPresent()) return cached.get();

        var context = historyService.loadContext(conversation.getId(), requestId);
        historyService.saveUserMessage(userId, conversation.getId(), requestId, AiMessageType.IMAGE, userInstruction);
        Media media =
                Media.builder().data(file.getResource()).mimeType(mimeType).build();

        QuestionImageModelResult generated;
        try {
            generated = chatClient
                    .prompt()
                    .system(IMAGE_EXPLANATION_SYSTEM_PROMPT)
                    .messages(context)
                    .user(user -> user.text(userInstruction).media(media))
                    .call()
                    .entity(QuestionImageModelResult.class);
        } catch (TransientAiException | NonTransientAiException exception) {
            historyService.saveFailedAssistant(
                    userId, conversation.getId(), requestId, AiMessageType.QUESTION_EXPLANATION);
            throw exception;
        } catch (RuntimeException exception) {
            historyService.saveFailedAssistant(
                    userId, conversation.getId(), requestId, AiMessageType.QUESTION_EXPLANATION);
            throw new AppException(ErrorCode.INVALID_AI_RESPONSE);
        }

        if (generated == null || generated.question() == null) {
            historyService.saveFailedAssistant(
                    userId, conversation.getId(), requestId, AiMessageType.QUESTION_EXPLANATION);
            throw new AppException(ErrorCode.INVALID_AI_RESPONSE);
        }

        QuestionImageExplanationResponse response = new QuestionImageExplanationResponse(
                conversation.getId().toString(),
                requestId,
                UUID.randomUUID().toString(),
                "QUESTION_EXPLANATION",
                Instant.now(),
                normalizeQuestion(generated.question()),
                generated.correctAnswer(),
                generated.translation(),
                generated.grammarAnalysis(),
                safeList(generated.optionAnalysis()),
                safeList(generated.vocabulary()),
                Math.max(0, Math.min(1, generated.confidence())),
                cleanStrings(generated.warnings()));
        historyService.saveAssistantMessage(
                userId,
                conversation.getId(),
                requestId,
                AiMessageType.QUESTION_EXPLANATION,
                imageAnswerSummary(response),
                response);
        return response;
    }

    public void archiveConversation(String userId, String conversationId) {
        historyService.archiveConversation(userId, conversationId);
    }

    private String requestId(String value) {
        return StringUtils.hasText(value) ? value.trim() : UUID.randomUUID().toString();
    }

    private String existingConversationId(String userId, String requestId, String conversationId) {
        if (StringUtils.hasText(conversationId)) return conversationId;
        return historyService.findConversationIdByRequest(userId, requestId).orElse(null);
    }

    private String imageAnswerSummary(QuestionImageExplanationResponse response) {
        if (response.correctAnswer() == null) return "Chưa đủ thông tin để xác định đáp án.";
        return "Đáp án " + response.correctAnswer().label() + ". "
                + response.correctAnswer().text();
    }

    private MimeType validateImage(MultipartFile file) {
        if (file == null || file.isEmpty() || file.getSize() > MAX_IMAGE_SIZE) {
            throw new AppException(ErrorCode.INVALID_AI_IMAGE);
        }

        String detectedType = detectImageType(file);
        if (!SUPPORTED_IMAGE_TYPES.contains(detectedType)) {
            throw new AppException(ErrorCode.INVALID_AI_IMAGE);
        }

        return MimeType.valueOf(detectedType);
    }

    private String detectImageType(MultipartFile file) {
        try (InputStream input = file.getInputStream()) {
            byte[] header = input.readNBytes(12);
            if (header.length >= 3
                    && unsigned(header[0]) == 0xFF
                    && unsigned(header[1]) == 0xD8
                    && unsigned(header[2]) == 0xFF) {
                return "image/jpeg";
            }
            if (header.length >= 8
                    && unsigned(header[0]) == 0x89
                    && header[1] == 'P'
                    && header[2] == 'N'
                    && header[3] == 'G') {
                return "image/png";
            }
            if (header.length >= 12
                    && header[0] == 'R'
                    && header[1] == 'I'
                    && header[2] == 'F'
                    && header[3] == 'F'
                    && header[8] == 'W'
                    && header[9] == 'E'
                    && header[10] == 'B'
                    && header[11] == 'P') {
                return "image/webp";
            }
            return "";
        } catch (IOException exception) {
            throw new AppException(ErrorCode.INVALID_AI_IMAGE);
        }
    }

    private int unsigned(byte value) {
        return value & 0xFF;
    }

    private TutorChatResponse.LanguageAnalysis normalizeAnalysis(
            TutorChatResponse.LanguageAnalysis analysis, String originalMessage) {
        if (analysis == null) {
            return new TutorChatResponse.LanguageAnalysis(
                    "unknown", originalMessage, originalMessage, List.of(), List.of());
        }

        return new TutorChatResponse.LanguageAnalysis(
                textOrDefault(analysis.detectedLanguage(), "unknown"),
                textOrDefault(analysis.originalMessage(), originalMessage),
                textOrDefault(analysis.correctedMessage(), originalMessage),
                safeList(analysis.grammarIssues()),
                safeList(analysis.vocabulary()));
    }

    private QuestionImageExplanationResponse.RecognizedQuestion normalizeQuestion(
            QuestionImageExplanationResponse.RecognizedQuestion question) {
        return new QuestionImageExplanationResponse.RecognizedQuestion(
                textOrDefault(question.content(), ""), safeList(question.options()));
    }

    private String textOrDefault(String value, String fallback) {
        return StringUtils.hasText(value) ? value.trim() : fallback;
    }

    private List<String> cleanStrings(List<String> values) {
        if (values == null) return List.of();
        return values.stream().filter(StringUtils::hasText).map(String::trim).toList();
    }

    private <T> List<T> safeList(List<T> values) {
        return values == null
                ? List.of()
                : values.stream().filter(java.util.Objects::nonNull).toList();
    }

    private record TutorChatModelResult(
            TutorChatResponse.LanguageAnalysis analysis, String reply, List<String> followUpSuggestions) {}

    private record QuestionImageModelResult(
            QuestionImageExplanationResponse.RecognizedQuestion question,
            QuestionImageExplanationResponse.CorrectAnswer correctAnswer,
            QuestionImageExplanationResponse.Translation translation,
            QuestionImageExplanationResponse.GrammarAnalysis grammarAnalysis,
            List<QuestionImageExplanationResponse.OptionAnalysis> optionAnalysis,
            List<QuestionImageExplanationResponse.VocabularyItem> vocabulary,
            double confidence,
            List<String> warnings) {}
}

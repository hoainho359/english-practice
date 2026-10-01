package org.example.supperapp.examservice.service.ai;

import java.util.Set;
import java.util.UUID;

import org.example.supperapp.examservice.dto.request.TutorChatRequest;
import org.example.supperapp.examservice.dto.response.QuestionImageExplanationResponse;
import org.example.supperapp.examservice.dto.response.TutorChatResponse;
import org.example.supperapp.examservice.exception.AppException;
import org.example.supperapp.examservice.exception.ErrorCode;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.content.Media;
import org.springframework.stereotype.Service;
import org.springframework.util.MimeType;
import org.springframework.util.MimeTypeUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ChatService {

    private static final long MAX_IMAGE_SIZE = 10 * 1024 * 1024;
    private static final Set<String> SUPPORTED_IMAGE_TYPES = Set.of("image/jpeg", "image/png", "image/webp");

    private static final String IMAGE_EXPLANATION_SYSTEM_PROMPT = """
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
            """;

    private static final String CONVERSATION_SYSTEM_PROMPT = """
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
            """;

    private final ChatClient chatClient;
    private final ChatMemory chatMemory;
    private final MessageChatMemoryAdvisor memoryAdvisor;

    public ChatService(ChatClient.Builder builder) {
        this.chatClient = builder.build();

        // Development-stage memory: retain only the latest 20 messages per
        // conversation so a long mobile chat does not grow the prompt forever.
        // It is intentionally in-memory and will be replaced by JDBC/Redis when
        // conversations must survive restarts or run on multiple service instances.
        this.chatMemory = MessageWindowChatMemory.builder().maxMessages(20).build();
        this.memoryAdvisor = MessageChatMemoryAdvisor.builder(chatMemory).build();
    }

    public TutorChatResponse chat(String userId, TutorChatRequest request) {
        String conversationId = StringUtils.hasText(request.conversationId())
                ? request.conversationId().trim()
                : UUID.randomUUID().toString();
        String memoryKey = memoryKey(userId, conversationId);

        TutorChatResponse generated = chatClient
                .prompt()
                .system(CONVERSATION_SYSTEM_PROMPT)
                .user(request.message())
                // The conversation id isolates one user's history from another.
                .advisors(advisor -> advisor
                        .advisors(memoryAdvisor)
                        .param(ChatMemory.CONVERSATION_ID, memoryKey))
                .call()
                .entity(TutorChatResponse.class);

        // Never trust the model to create or echo an infrastructure identifier.
        return new TutorChatResponse(
                conversationId,
                generated.analysis(),
                generated.reply(),
                generated.followUpSuggestions());
    }

    public QuestionImageExplanationResponse explainImage(MultipartFile file, String instruction) {
        validateImage(file);

        MimeType mimeType = MimeTypeUtils.parseMimeType(file.getContentType());
        Media media = Media.builder().data(file.getResource()).mimeType(mimeType).build();
        String userInstruction = StringUtils.hasText(instruction)
                ? instruction.trim()
                : "Read this TOEIC question image and explain it using all five required sections.";

        // Image solving is deliberately stateless: it must not leak into or consume
        // the history of a conversational tutoring session.
        return chatClient
                .prompt()
                .system(IMAGE_EXPLANATION_SYSTEM_PROMPT)
                .user(user -> user.text(userInstruction).media(media))
                .call()
                .entity(QuestionImageExplanationResponse.class);
    }

    public void clearConversation(String userId, String conversationId) {
        if (StringUtils.hasText(conversationId)) {
            chatMemory.clear(memoryKey(userId, conversationId.trim()));
        }
    }

    private String memoryKey(String userId, String conversationId) {
        // Scope memory by the authenticated principal so another user cannot
        // attach to a conversation by guessing its public UUID.
        return userId + ":" + conversationId;
    }

    private void validateImage(MultipartFile file) {
        if (file == null || file.isEmpty() || file.getSize() > MAX_IMAGE_SIZE) {
            throw new AppException(ErrorCode.INVALID_AI_IMAGE);
        }

        String contentType = file.getContentType();
        if (!StringUtils.hasText(contentType) || !SUPPORTED_IMAGE_TYPES.contains(contentType.toLowerCase())) {
            throw new AppException(ErrorCode.INVALID_AI_IMAGE);
        }
    }
}

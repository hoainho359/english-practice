package org.example.supperapp.examservice.service.ai;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.example.supperapp.examservice.dto.response.AiConversationMessagesResponse;
import org.example.supperapp.examservice.dto.response.AiConversationPageResponse;
import org.example.supperapp.examservice.dto.response.AiConversationSummaryResponse;
import org.example.supperapp.examservice.dto.response.AiHistoryMessageResponse;
import org.example.supperapp.examservice.dto.response.QuestionImageExplanationResponse;
import org.example.supperapp.examservice.dto.response.TutorChatResponse;
import org.example.supperapp.examservice.entity.ai.AiConversationEntity;
import org.example.supperapp.examservice.entity.ai.AiConversationStatus;
import org.example.supperapp.examservice.entity.ai.AiMessageEntity;
import org.example.supperapp.examservice.entity.ai.AiMessageRole;
import org.example.supperapp.examservice.entity.ai.AiMessageStatus;
import org.example.supperapp.examservice.entity.ai.AiMessageType;
import org.example.supperapp.examservice.exception.AppException;
import org.example.supperapp.examservice.exception.ErrorCode;
import org.example.supperapp.examservice.repository.AiConversationRepository;
import org.example.supperapp.examservice.repository.AiMessageRepository;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ChatHistoryService {

    static final int MAX_CONTEXT_MESSAGES = 20;

    AiConversationRepository conversationRepository;
    AiMessageRepository messageRepository;
    ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public Optional<String> findConversationIdByRequest(String userId, String requestId) {
        return messageRepository
                .findFirstByRequestIdAndConversationUserIdOrderByCreatedAtAsc(requestId, userId)
                .map(message -> message.getConversation().getId().toString());
    }

    @Transactional
    public AiConversationEntity resolveConversation(String userId, String conversationId, String firstMessage) {
        if (!StringUtils.hasText(conversationId)) {
            Instant now = Instant.now();
            return conversationRepository.save(AiConversationEntity.builder()
                    .userId(userId)
                    .title(buildTitle(firstMessage))
                    .status(AiConversationStatus.ACTIVE)
                    .lastMessagePreview(preview(firstMessage))
                    .messageCount(0)
                    .lastMessageAt(now)
                    .build());
        }

        return ownedConversation(userId, parseConversationId(conversationId));
    }

    @Transactional(readOnly = true)
    public Optional<TutorChatResponse> findTutorResponse(UUID conversationId, String requestId) {
        return completedAssistant(conversationId, requestId, AiMessageType.TUTOR_REPLY)
                .flatMap(message -> readPayload(message.getStructuredPayload(), TutorChatResponse.class));
    }

    @Transactional(readOnly = true)
    public Optional<QuestionImageExplanationResponse> findImageResponse(UUID conversationId, String requestId) {
        return completedAssistant(conversationId, requestId, AiMessageType.QUESTION_EXPLANATION)
                .flatMap(
                        message -> readPayload(message.getStructuredPayload(), QuestionImageExplanationResponse.class));
    }

    @Transactional(readOnly = true)
    public List<Message> loadContext(UUID conversationId, String excludedRequestId) {
        var slice = messageRepository.findByConversationIdAndStatusOrderByCreatedAtDesc(
                conversationId, AiMessageStatus.COMPLETED, PageRequest.of(0, MAX_CONTEXT_MESSAGES + 4));

        List<Message> result = new ArrayList<>();
        slice.getContent().stream()
                .filter(message -> !message.getRequestId().equals(excludedRequestId))
                .filter(message -> StringUtils.hasText(message.getContent()))
                .limit(MAX_CONTEXT_MESSAGES)
                .forEach(message -> result.add(
                        message.getRole() == AiMessageRole.USER
                                ? new UserMessage(message.getContent())
                                : new AssistantMessage(message.getContent())));
        Collections.reverse(result);
        return result;
    }

    @Transactional
    public void saveUserMessage(
            String userId, UUID conversationId, String requestId, AiMessageType type, String content) {
        AiConversationEntity conversation = ownedConversation(userId, conversationId);
        if (messageRepository
                .findByConversationIdAndRequestIdAndRole(conversationId, requestId, AiMessageRole.USER)
                .isPresent()) {
            return;
        }

        saveMessage(conversation, requestId, AiMessageRole.USER, type, AiMessageStatus.COMPLETED, content, null);
    }

    @Transactional
    public void saveAssistantMessage(
            String userId,
            UUID conversationId,
            String requestId,
            AiMessageType type,
            String content,
            Object structuredPayload) {
        AiConversationEntity conversation = ownedConversation(userId, conversationId);
        Optional<AiMessageEntity> existing = messageRepository.findByConversationIdAndRequestIdAndRole(
                conversationId, requestId, AiMessageRole.ASSISTANT);
        String payload = writePayload(structuredPayload);

        if (existing.isPresent()) {
            AiMessageEntity message = existing.get();
            message.setMessageType(type);
            message.setStatus(AiMessageStatus.COMPLETED);
            message.setContent(content);
            message.setStructuredPayload(payload);
            updateConversation(conversation, content, false);
            return;
        }

        saveMessage(
                conversation, requestId, AiMessageRole.ASSISTANT, type, AiMessageStatus.COMPLETED, content, payload);
    }

    @Transactional
    public void saveFailedAssistant(String userId, UUID conversationId, String requestId, AiMessageType type) {
        AiConversationEntity conversation = ownedConversation(userId, conversationId);
        Optional<AiMessageEntity> existing = messageRepository.findByConversationIdAndRequestIdAndRole(
                conversationId, requestId, AiMessageRole.ASSISTANT);
        if (existing.filter(message -> message.getStatus() == AiMessageStatus.COMPLETED)
                .isPresent()) {
            return;
        }
        if (existing.isPresent()) {
            existing.get().setStatus(AiMessageStatus.FAILED);
            return;
        }

        saveMessage(
                conversation,
                requestId,
                AiMessageRole.ASSISTANT,
                type,
                AiMessageStatus.FAILED,
                "Không thể tạo phản hồi. Vui lòng thử lại.",
                null);
    }

    @Transactional
    public AiConversationPageResponse getConversations(String userId, int page, int size) {
        var result = conversationRepository.findByUserIdAndStatusOrderByLastMessageAtDesc(
                userId, AiConversationStatus.ACTIVE, PageRequest.of(page, size));
        result.getContent().forEach(this::repairBrokenUnicode);
        var items = result.getContent().stream()
                .map(conversation -> new AiConversationSummaryResponse(
                        conversation.getId().toString(),
                        conversation.getTitle(),
                        conversation.getLastMessagePreview(),
                        conversation.getMessageCount(),
                        conversation.getLastMessageAt()))
                .toList();
        return new AiConversationPageResponse(
                items, result.getNumber(), result.getSize(), result.getTotalElements(), result.hasNext());
    }

    private void repairBrokenUnicode(AiConversationEntity conversation) {
        if (conversation.getTitle().contains("?")) {
            messageRepository
                    .findFirstByConversationIdAndRoleOrderByCreatedAtAsc(conversation.getId(), AiMessageRole.USER)
                    .map(message -> message.getMessageType() == AiMessageType.IMAGE
                            ? "Giải thích ảnh câu hỏi TOEIC"
                            : buildTitle(message.getContent()))
                    .ifPresent(conversation::setTitle);
        }
        if (conversation.getLastMessagePreview() != null
                && conversation.getLastMessagePreview().contains("?")) {
            messageRepository
                    .findFirstByConversationIdOrderByCreatedAtDesc(conversation.getId())
                    .map(AiMessageEntity::getContent)
                    .map(this::preview)
                    .ifPresent(conversation::setLastMessagePreview);
        }
    }

    @Transactional(readOnly = true)
    public AiConversationMessagesResponse getMessages(String userId, String conversationId, Instant before, int limit) {
        AiConversationEntity conversation = ownedConversation(userId, parseConversationId(conversationId));
        var pageable = PageRequest.of(0, limit);
        var slice = before == null
                ? messageRepository.findByConversationIdOrderByCreatedAtDesc(conversation.getId(), pageable)
                : messageRepository.findByConversationIdAndCreatedAtLessThanOrderByCreatedAtDesc(
                        conversation.getId(), before, pageable);

        List<AiHistoryMessageResponse> messages = new ArrayList<>(
                slice.getContent().stream().map(this::toHistoryResponse).toList());
        Collections.reverse(messages);
        String nextCursor = slice.hasNext() && !messages.isEmpty()
                ? messages.getFirst().createdAt().toString()
                : null;
        return new AiConversationMessagesResponse(
                conversation.getId().toString(), conversation.getTitle(), messages, nextCursor);
    }

    @Transactional
    public void archiveConversation(String userId, String conversationId) {
        AiConversationEntity conversation = ownedConversation(userId, parseConversationId(conversationId));
        conversation.setStatus(AiConversationStatus.ARCHIVED);
    }

    private Optional<AiMessageEntity> completedAssistant(
            UUID conversationId, String requestId, AiMessageType expectedType) {
        return messageRepository
                .findByConversationIdAndRequestIdAndRole(conversationId, requestId, AiMessageRole.ASSISTANT)
                .filter(message -> message.getStatus() == AiMessageStatus.COMPLETED)
                .filter(message -> message.getMessageType() == expectedType);
    }

    private void saveMessage(
            AiConversationEntity conversation,
            String requestId,
            AiMessageRole role,
            AiMessageType type,
            AiMessageStatus status,
            String content,
            String payload) {
        messageRepository.save(AiMessageEntity.builder()
                .conversation(conversation)
                .requestId(requestId)
                .role(role)
                .messageType(type)
                .status(status)
                .content(content)
                .structuredPayload(payload)
                .build());
        updateConversation(conversation, content, true);
    }

    private void updateConversation(AiConversationEntity conversation, String content, boolean increment) {
        conversation.setLastMessageAt(Instant.now());
        conversation.setLastMessagePreview(preview(content));
        if (increment) conversation.setMessageCount(conversation.getMessageCount() + 1);
    }

    private AiHistoryMessageResponse toHistoryResponse(AiMessageEntity message) {
        TutorChatResponse.LanguageAnalysis analysis = null;
        QuestionImageExplanationResponse explanation = null;
        if (message.getMessageType() == AiMessageType.TUTOR_REPLY) {
            analysis = readPayload(message.getStructuredPayload(), TutorChatResponse.class)
                    .map(TutorChatResponse::analysis)
                    .orElse(null);
        } else if (message.getMessageType() == AiMessageType.QUESTION_EXPLANATION) {
            explanation = readPayload(message.getStructuredPayload(), QuestionImageExplanationResponse.class)
                    .orElse(null);
        }
        return new AiHistoryMessageResponse(
                message.getId().toString(),
                message.getRequestId(),
                message.getRole(),
                message.getMessageType(),
                message.getStatus(),
                message.getContent(),
                analysis,
                explanation,
                message.getCreatedAt());
    }

    private AiConversationEntity ownedConversation(String userId, UUID conversationId) {
        return conversationRepository
                .findByIdAndUserIdAndStatus(conversationId, userId, AiConversationStatus.ACTIVE)
                .orElseThrow(() -> new AppException(ErrorCode.AI_CONVERSATION_NOT_FOUND));
    }

    private UUID parseConversationId(String value) {
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException exception) {
            throw new AppException(ErrorCode.INVALID_AI_CONVERSATION_ID);
        }
    }

    private String writePayload(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new AppException(ErrorCode.INVALID_AI_RESPONSE);
        }
    }

    private <T> Optional<T> readPayload(String value, Class<T> type) {
        if (!StringUtils.hasText(value)) return Optional.empty();
        try {
            return Optional.of(objectMapper.readValue(value, type));
        } catch (JsonProcessingException exception) {
            return Optional.empty();
        }
    }

    private String buildTitle(String value) {
        String normalized = StringUtils.hasText(value) ? value.trim().replaceAll("\\s+", " ") : "Cuộc trò chuyện mới";
        return normalized.length() <= 80 ? normalized : normalized.substring(0, 77) + "...";
    }

    private String preview(String value) {
        String normalized = StringUtils.hasText(value) ? value.trim().replaceAll("\\s+", " ") : "";
        return normalized.length() <= 260 ? normalized : normalized.substring(0, 257) + "...";
    }
}

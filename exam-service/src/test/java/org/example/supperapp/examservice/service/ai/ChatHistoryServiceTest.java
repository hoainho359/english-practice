package org.example.supperapp.examservice.service.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.List;

import jakarta.persistence.EntityManager;

import org.example.supperapp.examservice.dto.response.TutorChatResponse;
import org.example.supperapp.examservice.entity.ai.AiMessageType;
import org.example.supperapp.examservice.exception.AppException;
import org.example.supperapp.examservice.exception.ErrorCode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class ChatHistoryServiceTest {

    @Autowired
    ChatHistoryService historyService;

    @Autowired
    EntityManager entityManager;

    @Test
    void savesListsAndReloadsConversationMessages() {
        var conversation = historyService.resolveConversation("user-1", null, "Please check my grammar");
        historyService.saveUserMessage(
                "user-1", conversation.getId(), "request-1", AiMessageType.TEXT, "I goes to school");
        historyService.saveUserMessage(
                "user-1", conversation.getId(), "request-1", AiMessageType.TEXT, "I goes to school");

        var response = new TutorChatResponse(
                conversation.getId().toString(),
                "request-1",
                "assistant-1",
                "TUTOR_REPLY",
                new TutorChatResponse.LanguageAnalysis(
                        "en", "I goes to school", "I go to school", List.of(), List.of()),
                "Use 'go' with the subject 'I'.",
                List.of("Try another sentence"),
                Instant.now());
        historyService.saveAssistantMessage(
                "user-1", conversation.getId(), "request-1", AiMessageType.TUTOR_REPLY, response.reply(), response);

        var conversations = historyService.getConversations("user-1", 0, 20);
        var messages = historyService.getMessages("user-1", conversation.getId().toString(), null, 30);

        assertThat(conversations.items()).hasSize(1);
        assertThat(conversations.items().getFirst().messageCount()).isEqualTo(2);
        assertThat(historyService.findConversationIdByRequest("user-1", "request-1"))
                .contains(conversation.getId().toString());
        assertThat(messages.messages()).hasSize(2);
        assertThat(messages.messages().getLast().analysis().correctedMessage()).isEqualTo("I go to school");

        historyService.archiveConversation("user-1", conversation.getId().toString());
        assertThat(historyService.getConversations("user-1", 0, 20).items()).isEmpty();
    }

    @Test
    void doesNotExposeAnotherUsersConversation() {
        var conversation = historyService.resolveConversation("owner", null, "Private chat");

        assertThatThrownBy(() -> historyService.getMessages(
                        "other-user", conversation.getId().toString(), null, 30))
                .isInstanceOf(AppException.class)
                .extracting(exception -> ((AppException) exception).getErrorCode())
                .isEqualTo(ErrorCode.AI_CONVERSATION_NOT_FOUND);
    }

    @Test
    void repairsBrokenVietnameseSummaryFromUnicodeMessages() {
        String userMessage = "Giải thích câu hỏi trong ảnh này theo năm phần.";
        String expectedTitle = "Giải thích ảnh câu hỏi TOEIC";
        String failedReply = "Không thể tạo phản hồi. Vui lòng thử lại.";
        var conversation = historyService.resolveConversation("user-utf8", null, expectedTitle);
        historyService.saveUserMessage(
                "user-utf8", conversation.getId(), "request-utf8", AiMessageType.IMAGE, userMessage);
        historyService.saveFailedAssistant(
                "user-utf8", conversation.getId(), "request-utf8", AiMessageType.TUTOR_REPLY);

        // Mo phong du lieu cu da bi SQL Server doi ky tu tieng Viet thanh dau hoi.
        conversation.setTitle("Gi?i thich ?nh cau h?i TOEIC");
        conversation.setLastMessagePreview("Khong th? t?o ph?n h?i. Vui long th? l?i.");
        entityManager.flush();
        entityManager.clear();

        var result = historyService.getConversations("user-utf8", 0, 20);

        assertThat(result.items()).hasSize(1);
        assertThat(result.items().getFirst().title()).isEqualTo(expectedTitle);
        assertThat(result.items().getFirst().lastMessagePreview()).isEqualTo(failedReply);
    }
}

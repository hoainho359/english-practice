package org.example.supperapp.examservice.controller.ai;

import java.time.Instant;

import jakarta.validation.Valid;

import org.example.supperapp.examservice.dto.request.TutorChatRequest;
import org.example.supperapp.examservice.dto.response.AiConversationMessagesResponse;
import org.example.supperapp.examservice.dto.response.AiConversationPageResponse;
import org.example.supperapp.examservice.dto.response.ApiResponse;
import org.example.supperapp.examservice.dto.response.QuestionImageExplanationResponse;
import org.example.supperapp.examservice.dto.response.TutorChatResponse;
import org.example.supperapp.examservice.service.ai.ChatHistoryService;
import org.example.supperapp.examservice.service.ai.ChatService;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@RestController
@RequestMapping("/chat")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
public class ChatController {
    ChatService chatService;
    ChatHistoryService historyService;

    /** Gui lai conversationId de AI nho cac tin nhan truoc. */
    @PostMapping(
            value = {"", "/"},
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ApiResponse<TutorChatResponse> chat(
            @Valid @RequestBody TutorChatRequest request, Authentication authentication) {
        return ApiResponse.<TutorChatResponse>builder()
                .message("Tutor response generated successfully")
                .result(chatService.chat(authentication.getName(), request))
                .build();
    }

    /** Giai anh khong can questionId, chi luu metadata vao lich su chat. */
    @PostMapping(
            value = "/image",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ApiResponse<QuestionImageExplanationResponse> explainImage(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "message", required = false) String message,
            @RequestParam(value = "conversationId", required = false) String conversationId,
            @RequestParam(value = "clientMessageId", required = false) String clientMessageId,
            Authentication authentication) {
        return ApiResponse.<QuestionImageExplanationResponse>builder()
                .message("Question image explained successfully")
                .result(chatService.explainImage(
                        authentication.getName(), file, message, conversationId, clientMessageId))
                .build();
    }

    @GetMapping("/conversations")
    public ApiResponse<AiConversationPageResponse> getConversations(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication) {
        int safePage = Math.max(0, page);
        int safeSize = Math.max(1, Math.min(size, 50));
        return ApiResponse.<AiConversationPageResponse>builder()
                .message("Conversations loaded successfully")
                .result(historyService.getConversations(authentication.getName(), safePage, safeSize))
                .build();
    }

    @GetMapping("/conversations/{conversationId}/messages")
    public ApiResponse<AiConversationMessagesResponse> getMessages(
            @PathVariable String conversationId,
            @RequestParam(required = false) Instant before,
            @RequestParam(defaultValue = "30") int limit,
            Authentication authentication) {
        int safeLimit = Math.max(1, Math.min(limit, 50));
        return ApiResponse.<AiConversationMessagesResponse>builder()
                .message("Conversation messages loaded successfully")
                .result(historyService.getMessages(authentication.getName(), conversationId, before, safeLimit))
                .build();
    }

    /** Chi archive khi nguoi dung chon xoa trong man hinh lich su. */
    @DeleteMapping("/conversations/{conversationId}")
    public ApiResponse<Void> archiveConversation(@PathVariable String conversationId, Authentication authentication) {
        chatService.archiveConversation(authentication.getName(), conversationId);
        return ApiResponse.<Void>builder()
                .message("Conversation archived successfully")
                .build();
    }
}

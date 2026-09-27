package org.example.supperapp.examservice.controller.ai;

import jakarta.validation.Valid;

import org.example.supperapp.examservice.dto.request.TutorChatRequest;
import org.example.supperapp.examservice.dto.response.ApiResponse;
import org.example.supperapp.examservice.dto.response.QuestionImageExplanationResponse;
import org.example.supperapp.examservice.dto.response.TutorChatResponse;
import org.example.supperapp.examservice.service.ai.ChatService;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
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

    /** Multi-turn tutor chat. Reuse result.conversationId in the next request. */
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

    /** Stateless TOEIC image explanation; no questionId or database record is required. */
    @PostMapping(
            value = "/image",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ApiResponse<QuestionImageExplanationResponse> explainImage(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "message", required = false) String message) {
        return ApiResponse.<QuestionImageExplanationResponse>builder()
                .message("Question image explained successfully")
                .result(chatService.explainImage(file, message))
                .build();
    }

    /** Allows the history/reset button in Expo to start the conversation again. */
    @DeleteMapping("/conversations/{conversationId}")
    public ApiResponse<Void> clearConversation(
            @PathVariable String conversationId, Authentication authentication) {
        chatService.clearConversation(authentication.getName(), conversationId);
        return ApiResponse.<Void>builder().message("Conversation cleared successfully").build();
    }
}

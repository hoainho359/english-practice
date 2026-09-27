package org.example.supperapp.examservice.controller.ai;

import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.example.supperapp.examservice.dto.request.ChatRequest;
import org.example.supperapp.examservice.service.ai.ChatService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/chat")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
public class ChatController {
    ChatService chatService;

    // Both "" and "/" are declared because Spring Framework 6 no longer
    // performs implicit trailing-slash matching. The full direct URL is
    // /exam-service/chat; through the gateway it is /api/exam-service/chat.
    @PostMapping(
            value = {"", "/"},
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.TEXT_PLAIN_VALUE)
    public String chat(@Valid @RequestBody ChatRequest chatRequest) {
        return chatService.chat(chatRequest);
    }
}

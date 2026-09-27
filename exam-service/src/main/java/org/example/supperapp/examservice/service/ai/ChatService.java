package org.example.supperapp.examservice.service.ai;

import org.example.supperapp.examservice.dto.request.ChatRequest;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

@Service
public class ChatService {
    private final ChatClient chatClient;

    public ChatService(ChatClient.Builder builder) {
        // ChatClient itself is not auto-registered as a bean. Spring AI provides
        // ChatClient.Builder after a compatible ChatModel has been configured.
        // Building it once here also avoids rebuilding a client for every request.
        this.chatClient = builder.build();
    }

    public String chat(ChatRequest chatRequest) {
        return chatClient
                .prompt()
                .system("You are a TOEIC tutor. Explain clearly, accurately, and concisely.")
                .user(chatRequest.message())
                .call()
                .content();
    }
}

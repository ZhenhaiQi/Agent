package com.example.agent.chat;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 阶段 1 最小可用：一次性对话。
 *
 * <pre>
 * curl -X POST http://localhost:8080/api/chat \
 *   -H "Content-Type: application/json" \
 *   -d '{"message":"用一句话解释什么是 Agent"}'
 * </pre>
 *
 * <p>用 POST + JSON body 而不是 GET query，是因为 LLM 的输入是任意长文本，
 * 放进 query string 会有编码、长度、日志泄漏等问题。
 */
@RestController
@RequestMapping("/api")
public class ChatController {

    /** 请求体 */
    public record ChatRequest(@NotBlank(message = "不能为空") String message) {
    }

    private final ChatClient chatClient;

    public ChatController(ChatClient.Builder builder) {
        this.chatClient = builder
                .defaultSystem("你是一个简洁的中文助手，回答保持简短。")
                .build();
    }

    @PostMapping("/chat")
    public String chat(@Valid @RequestBody ChatRequest request) {
        return chatClient.prompt()
                .user(request.message())
                .call()
                .content();
    }
}

package com.example.agent.chat;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 阶段 1 第二个练习：结构化输出（文本 → JSON）。
 *
 * <pre>
 * curl -X POST http://localhost:8080/api/extract \
 *   -H "Content-Type: application/json" \
 *   -d '{"text":"张三今年30岁，住在杭州"}'
 * → {"name":"张三","age":30,"city":"杭州"}
 * </pre>
 *
 * <p>底层由 Spring AI 的 BeanOutputConverter 完成：自动把 record 转成 JSON Schema
 * 塞进提示词，再把模型输出反序列化成对象。
 */
@RestController
@RequestMapping("/api")
public class ExtractController {

    /** 请求体 */
    public record ExtractRequest(String text) {
    }

    /** 目标结构：字段缺失时留空 */
    public record PersonInfo(String name, Integer age, String city) {
    }

    private final ChatClient chatClient;

    public ExtractController(ChatClient.Builder builder) {
        this.chatClient = builder.build();
    }

    @PostMapping("/extract")
    public PersonInfo extract(@RequestBody ExtractRequest request) {
        return chatClient.prompt()
                .system("从用户提供的文本中抽取人物信息，信息缺失的字段留空。")
                .user(request.text())
                .call()
                .entity(PersonInfo.class);
    }
}

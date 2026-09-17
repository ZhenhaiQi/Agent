package com.example.agent;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

// 测试里用占位 key，让 contextLoads 不依赖真实凭证
@SpringBootTest(properties = "spring.ai.openai.api-key=test-placeholder")
class AgentApplicationTests {

    @Test
    void contextLoads() {
    }

}

package cn.edu.sdua.zhinengbook.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 大模型API接入配置
 */
@ConfigurationProperties(prefix = "llm")
public record LlmProperties(
        String provider,
        String baseUrl,
        String model,
        String apiKey,
        int timeoutSeconds,
        boolean mockWhenNoKey,
        boolean fallbackToMockOnError
) {
}

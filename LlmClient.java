package cn.edu.sdua.zhinengbook.service;

import cn.edu.sdua.zhinengbook.config.LlmProperties;
import cn.edu.sdua.zhinengbook.entity.ApiKeyConfig;
import cn.edu.sdua.zhinengbook.exception.ApiException;
import cn.edu.sdua.zhinengbook.repository.ApiKeyRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 大模型API高效集成：兼容 OpenAI / DashScope / DeepSeek 等
 * OpenAI Chat Completions 协议的供应商。
 * 密钥优先取数据库中激活的密钥，其次取 application.yml 配置；
 * 均无密钥且允许时降级为本地模拟生成，保证平台可离线演示。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LlmClient {

    public record LlmResult(String text, int promptTokens, int completionTokens, boolean realCall) {

        public int totalTokens() {
            return promptTokens + completionTokens;
        }
    }

    private final RestClient.Builder restClientBuilder;
    private final ApiKeyRepository apiKeyRepository;
    private final LlmProperties llmProperties;
    private final MockLlmGenerator mockLlmGenerator;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public LlmResult chat(List<PromptBuilder.ChatMessage> messages) {
        Optional<ApiKeyConfig> key = apiKeyRepository.findFirstByActiveTrue();
        String apiKey = key.map(ApiKeyConfig::getApiKey)
                .filter(s -> s != null && !s.isBlank())
                .orElse(llmProperties.apiKey());
        String baseUrl = key.map(ApiKeyConfig::getBaseUrl)
                .filter(s -> s != null && !s.isBlank())
                .orElse(llmProperties.baseUrl());
        String model = key.map(ApiKeyConfig::getModel)
                .filter(s -> s != null && !s.isBlank())
                .orElse(llmProperties.model());

        if (apiKey == null || apiKey.isBlank()) {
            if (llmProperties.mockWhenNoKey()) {
                log.debug("未配置API密钥，使用本地模拟生成");
                return mockLlmGenerator.generate(messages);
            }
            throw new ApiException("未配置大模型API密钥，请联系管理员在「密钥管理」中添加");
        }
        try {
            return callRemote(baseUrl, apiKey, model, messages);
        } catch (Exception e) {
            if (llmProperties.fallbackToMockOnError()) {
                log.warn("大模型真实调用失败，降级为本地模拟生成: {}", e.getMessage());
                return mockLlmGenerator.generate(messages);
            }
            throw e;
        }
    }

    /** 连通性测试（密钥管理页「测试」按钮使用） */
    public LlmResult testConnection(String baseUrl, String apiKey, String model) {
        return callRemote(baseUrl, apiKey, model,
                List.of(new PromptBuilder.ChatMessage("user", "回复「OK」两个字母即可。")));
    }

    private LlmResult callRemote(String baseUrl, String apiKey, String model,
                                 List<PromptBuilder.ChatMessage> messages) {
        try {
            String body = objectMapper.writeValueAsString(Map.of(
                    "model", model,
                    "messages", messages.stream()
                            .map(m -> Map.of("role", m.role(), "content", m.content()))
                            .toList(),
                    "temperature", 0.7
            ));
            String resp = restClientBuilder.build().post()
                    .uri(baseUrl.endsWith("/v1") || baseUrl.endsWith("/v1/")
                            ? baseUrl + "chat/completions"
                            : (baseUrl.endsWith("/") ? baseUrl : baseUrl + "/") + "chat/completions")
                    .header("Authorization", "Bearer " + apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(String.class);

            JsonNode node = objectMapper.readTree(resp);
            JsonNode choice = node.path("choices").path(0).path("message").path("content");
            if (choice.isMissingNode() || choice.asText().isBlank()) {
                throw new ApiException("大模型返回内容为空");
            }
            JsonNode usage = node.path("usage");
            int pt = usage.path("prompt_tokens").asInt(estimateTokens(messagesToString(messages)));
            int ct = usage.path("completion_tokens").asInt(estimateTokens(choice.asText()));
            return new LlmResult(choice.asText(), pt, ct, true);
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            log.error("大模型调用失败", e);
            throw new ApiException("大模型调用失败：" + e.getMessage());
        }
    }

    private String messagesToString(List<PromptBuilder.ChatMessage> messages) {
        StringBuilder sb = new StringBuilder();
        messages.forEach(m -> sb.append(m.content()));
        return sb.toString();
    }

    /** 粗略token估算：中文约1.5字符/token，用于未返回usage时兜底 */
    static int estimateTokens(String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }
        return Math.max(1, (int) (text.length() / 1.5));
    }
}

package cn.edu.sdua.zhinengbook.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.Map;

/**
 * 请求DTO集合
 */
public final class Dtos {

    private Dtos() {
    }

    public record RegisterRequest(
            @NotBlank String username,
            @NotBlank String password,
            String nickname) {
    }

    public record LoginRequest(@NotBlank String username, @NotBlank String password) {
    }

    /** 核心写作-生成 */
    public record GenerateRequest(
            Long templateId,
            Map<String, String> vars,
            String title) {
    }

    /** 文本润色与优化 */
    public record PolishRequest(
            Long workId,
            @NotBlank String text,
            String requirement,
            Boolean saveAsVersion) {
    }

    /** 实时推荐 */
    public record RecommendRequest(String text, String scene) {
    }

    /** 保存/更新作品 */
    public record WorkSaveRequest(
            Long id,
            @NotBlank @Size(max = 200) String title,
            String content,
            Long templateId,
            String category,
            String note) {
    }

    /** 模板管理 */
    public record TemplateRequest(
            @NotBlank String name,
            @NotBlank String category,
            String description,
            String systemPrompt,
            String promptTemplate,
            String variables,
            Boolean enabled) {
    }

    /** API密钥管理 */
    public record ApiKeyRequest(
            Long id,
            @NotBlank String name,
            @NotBlank String provider,
            @NotBlank String baseUrl,
            @NotBlank String model,
            String apiKey,
            String remark) {
    }

    /** 版本回滚 */
    public record RestoreRequest(Integer versionNo) {
    }
}

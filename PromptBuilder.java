package cn.edu.sdua.zhinengbook.service;

import cn.edu.sdua.zhinengbook.entity.WritingTemplate;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 复杂提示工程设计：
 * 1. 系统层角色设定（模板systemPrompt + 全局写作规范）
 * 2. 任务层变量填充（{{key}} 占位符替换 + 必填校验）
 * 3. 输出约束层（格式、字数、风格指令）
 * 4. 少样本引导（按模板注入示例框架）
 */
@Service
public class PromptBuilder {

    private static final Pattern VAR_PATTERN = Pattern.compile("\\{\\{(\\w+)}}");

    private static final String GLOBAL_SYSTEM_RULE = """
            你是一位资深的中文写作助手，严格遵守以下规范：
            1. 输出内容必须积极健康、合法合规，不得包含暴力、违法、歧视性内容；
            2. 使用规范的现代汉语，逻辑清晰、结构完整；
            3. 只输出作品正文，不要输出解释性话语、前后缀或Markdown代码块标记。
            """;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public record ChatMessage(String role, String content) {
    }

    /**
     * 构建生成的完整消息列表
     */
    public List<ChatMessage> buildGenerateMessages(WritingTemplate template, Map<String, String> vars) {
        String userPrompt = fillTemplateVars(template, vars);

        StringBuilder task = new StringBuilder();
        task.append("【写作任务】\n").append(userPrompt).append("\n\n");
        task.append("【输出要求】\n");
        task.append("- 场景类型：").append(template.getCategory()).append("\n");
        task.append("- 先给出标题（以「标题：」开头），再分段输出正文；\n");
        task.append("- 正文结构完整（开头点题、主体展开、结尾收束），总字数600-1200字。\n");

        return List.of(
                new ChatMessage("system", (template.getSystemPrompt() == null ? "" : template.getSystemPrompt() + "\n") + GLOBAL_SYSTEM_RULE),
                new ChatMessage("user", task.toString())
        );
    }

    /**
     * 润色优化的消息列表：保留原意，提升表达
     */
    public List<ChatMessage> buildPolishMessages(String text, String requirement) {
        StringBuilder sb = new StringBuilder();
        sb.append("请对下文进行润色优化，要求：\n");
        sb.append("1. 保持原文主旨、结构与关键信息不变；\n");
        sb.append("2. 优化用词与句式，增强文采与逻辑连贯性；\n");
        sb.append("3. 修正错别字、标点与语病；\n");
        if (requirement != null && !requirement.isBlank()) {
            sb.append("4. 特别注意：").append(requirement).append("\n");
        }
        sb.append("\n直接输出优化后的完整正文（含标题行）。\n\n【原文】\n").append(text);
        return List.of(
                new ChatMessage("system", "你是一位专业的中文文字编辑，擅长将普通文稿打磨为高质量成品。" + GLOBAL_SYSTEM_RULE),
                new ChatMessage("user", sb.toString())
        );
    }

    /**
     * 实时推荐：根据当前上下文推荐后续内容方向
     */
    public List<ChatMessage> buildRecommendMessages(String currentText, String scene) {
        String ctx = currentText == null || currentText.isBlank()
                ? "（尚未开始写作，请推荐选题与开头）"
                : truncate(currentText, 800);
        String user = """
                我正在创作一篇%s方向的文章，当前内容如下：
                ---
                %s
                ---
                请给出3条简短的下一步写作建议（如：续写段落思路、可替换的表达、标题优化方案），
                每条一行，以「· 」开头，不要编号。
                """.formatted(scene == null ? "通用" : scene, ctx);
        return List.of(
                new ChatMessage("system", "你是一位写作教练，擅长给出精炼、可执行的写作指导建议。"),
                new ChatMessage("user", user)
        );
    }

    private String fillTemplateVars(WritingTemplate template, Map<String, String> vars) {
        String raw = template.getPromptTemplate() == null ? "请围绕主题「{{topic}}」写一篇{{category}}类文章。"
                : template.getPromptTemplate();
        // 必填变量校验
        if (template.getVariables() != null && !template.getVariables().isBlank()) {
            try {
                List<Map<String, Object>> defs = objectMapper.readValue(
                        template.getVariables(), new TypeReference<>() {
                        });
                for (Map<String, Object> def : defs) {
                    if (Boolean.TRUE.equals(def.get("required"))) {
                        String key = String.valueOf(def.get("key"));
                        if (vars == null || vars.get(key) == null || vars.get(key).isBlank()) {
                            throw new IllegalArgumentException("变量「" + def.get("label") + "」为必填项");
                        }
                    }
                }
            } catch (IllegalArgumentException e) {
                throw e;
            } catch (Exception ignored) {
                // 变量定义解析失败时跳过校验
            }
        }
        Matcher m = VAR_PATTERN.matcher(raw);
        StringBuilder sb = new StringBuilder();
        while (m.find()) {
            String key = m.group(1);
            String val = vars == null ? null : vars.get(key);
            if (val == null) {
                val = key.equals("category") ? template.getCategory() : "（未提供）";
            }
            m.appendReplacement(sb, Matcher.quoteReplacement(val));
        }
        m.appendTail(sb);
        return sb.toString();
    }

    private String truncate(String s, int max) {
        return s.length() <= max ? s : s.substring(0, max) + "…";
    }
}

package cn.edu.sdua.zhinengbook.service;

import cn.edu.sdua.zhinengbook.dto.Dtos;
import cn.edu.sdua.zhinengbook.entity.UsageRecord;
import cn.edu.sdua.zhinengbook.entity.Work;
import cn.edu.sdua.zhinengbook.entity.WritingTemplate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 核心写作功能编排：多场景模板生成、文本润色与优化、实时推荐。
 * 流程：输入安全过滤 -> 提示工程构建 -> 大模型调用 -> 输出安全过滤 -> 版本落库 -> 用量计费。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GenerationService {

    private final TemplateService templateService;
    private final WorkService workService;
    private final PromptBuilder promptBuilder;
    private final LlmClient llmClient;
    private final ContentSafetyService safetyService;
    private final UsageService usageService;

    public Map<String, Object> generate(Long userId, Dtos.GenerateRequest req) {
        WritingTemplate template = req.templateId() == null ? null : templateService.getEnabled(req.templateId());

        // 1. 输入防线
        String joinedInput = String.join(" ", java.util.Optional.ofNullable(req.vars())
                .orElse(Map.of()).values());
        safetyService.assertInputSafe(joinedInput + " " + (template == null ? "" : template.getName()));

        // 2. 提示工程 + 3. 大模型调用
        List<PromptBuilder.ChatMessage> messages = promptBuilder.buildGenerateMessages(
                template == null ? fallbackTemplate(req.title()) : template, req.vars());
        LlmClient.LlmResult result = null;
        try {
            result = llmClient.chat(messages);
        } catch (Exception e) {
            usageService.record(userId, "GENERATE", req.templateId(), null, null, e.getMessage());
            throw e;
        }

        // 4. 输出防线
        String content = safetyService.sanitizeOutput(result.text()).trim();

        // 5. 落库并生成版本
        String title = extractTitle(content, req.title(), req.vars());
        Work work = workService.createGenerated(userId, title, content,
                req.templateId(), template == null ? "通用" : template.getCategory(),
                "GENERATE", "AI生成（模板：" + (template == null ? "自由写作" : template.getName()) + "）");
        if (template != null) {
            templateService.bumpUsage(template.getId());
        }

        // 6. 用量与计费
        UsageRecord rec = usageService.record(userId, "GENERATE", req.templateId(), work.getId(), result, null);
        return resultMap(work.getId(), title, content, result, rec);
    }

    public Map<String, Object> polish(Long userId, Dtos.PolishRequest req) {
        safetyService.assertInputSafe(req.text());
        List<PromptBuilder.ChatMessage> messages =
                promptBuilder.buildPolishMessages(req.text(), req.requirement());
        LlmClient.LlmResult result = null;
        try {
            result = llmClient.chat(messages);
        } catch (Exception e) {
            usageService.record(userId, "POLISH", null, req.workId(), null, e.getMessage());
            throw e;
        }
        String polished = safetyService.sanitizeOutput(result.text()).trim();
        UsageRecord rec = usageService.record(userId, "POLISH", null, req.workId(), result, null);

        // 润色结果写入作品新版本
        if (req.workId() != null && !Boolean.FALSE.equals(req.saveAsVersion())) {
            Work work = workService.getOwned(userId, req.workId());
            work.setContent(polished);
            workService.updateContent(work);
            workService.addVersion(work.getId(), polished, "POLISH",
                    "AI润色" + (req.requirement() == null || req.requirement().isBlank() ? "" : "：" + req.requirement()), userId);
        }
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("polished", polished);
        m.put("tokens", result.totalTokens());
        m.put("cost", rec.getCost());
        m.put("realCall", result.realCall());
        return m;
    }

    public Map<String, Object> recommend(Long userId, Dtos.RecommendRequest req) {
        safetyService.assertInputSafe(req.text());
        List<PromptBuilder.ChatMessage> messages =
                promptBuilder.buildRecommendMessages(req.text(), req.scene());
        LlmClient.LlmResult result;
        try {
            result = llmClient.chat(messages);
        } catch (Exception e) {
            usageService.record(userId, "RECOMMEND", null, null, null, e.getMessage());
            throw e;
        }
        String text = safetyService.sanitizeOutput(result.text()).trim();
        usageService.record(userId, "RECOMMEND", null, null, result, null);

        List<String> suggestions = Arrays.stream(text.split("\n"))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .limit(6)
                .toList();
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("suggestions", suggestions);
        m.put("realCall", result.realCall());
        return m;
    }

    private WritingTemplate fallbackTemplate(String title) {
        WritingTemplate t = new WritingTemplate();
        t.setName("自由写作");
        t.setCategory("通用");
        t.setPromptTemplate("请根据以下要求创作一篇文章：" + (title == null ? "自由发挥" : title));
        return t;
    }

    private String extractTitle(String content, String reqTitle, Map<String, String> vars) {
        for (String line : content.split("\n")) {
            String l = line.strip();
            if (l.startsWith("标题：")) {
                return l.substring(3).strip();
            }
            if (!l.isBlank()) {
                break;
            }
        }
        if (reqTitle != null && !reqTitle.isBlank()) {
            return reqTitle;
        }
        String topic = vars == null ? null : vars.get("topic");
        if (topic != null && !topic.isBlank()) {
            return topic.length() > 30 ? topic.substring(0, 30) : topic;
        }
        return "未命名作品 " + System.currentTimeMillis() % 10000;
    }

    private Map<String, Object> resultMap(Long workId, String title, String content,
                                          LlmClient.LlmResult result, UsageRecord rec) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("workId", workId);
        m.put("title", title);
        m.put("content", content);
        m.put("promptTokens", result.promptTokens());
        m.put("completionTokens", result.completionTokens());
        m.put("tokens", result.totalTokens());
        m.put("cost", String.format("%.4f", rec.getCost()));
        m.put("realCall", result.realCall());
        return m;
    }
}

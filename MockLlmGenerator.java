package cn.edu.sdua.zhinengbook.service;

import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 本地模拟大模型：未配置API密钥时降级使用，
 * 保证平台在无外网/无密钥环境下仍可完整演示写作流程。
 */
@Component
public class MockLlmGenerator {

    public LlmClient.LlmResult generate(List<PromptBuilder.ChatMessage> messages) {
        String userPrompt = messages.stream()
                .filter(m -> "user".equals(m.role()))
                .reduce((a, b) -> b)
                .map(PromptBuilder.ChatMessage::content)
                .orElse("");
        String system = messages.stream()
                .filter(m -> "system".equals(m.role()))
                .findFirst()
                .map(PromptBuilder.ChatMessage::content)
                .orElse("");

        String text;
        if (system.contains("写作教练")) {
            text = mockRecommend();
        } else if (system.contains("文字编辑")) {
            text = mockPolish(userPrompt);
        } else {
            text = mockWrite(userPrompt);
        }
        int pt = LlmClient.estimateTokens(userPrompt + system);
        int ct = LlmClient.estimateTokens(text);
        return new LlmClient.LlmResult(text, pt, ct, false);
    }

    private String mockWrite(String prompt) {
        // 从【写作任务】段落中提取主题，避免误拾输出要求中的示例符号
        String task = prompt;
        int s = prompt.indexOf("【写作任务】");
        int e = prompt.indexOf("【输出要求】");
        if (s >= 0) {
            task = prompt.substring(s + 6, e > s ? e : prompt.length()).trim();
        }
        String topic = extractBetween(task, "「", "」");
        if (topic == null || topic.isBlank()) {
            topic = task.isEmpty() ? null : task.split("\n")[0].trim();
        }
        if (topic == null || topic.isBlank()) {
            topic = "我的创作";
        }
        // 去除提示词句式，只保留核心主题
        topic = topic.replaceAll("^[请为我撰写一篇以主题围绕写作要求：:，。]+", "")
                .replaceAll("[。！!？?]+$", "").trim();
        topic = topic.length() > 30 ? topic.substring(0, 30) : topic;
        return """
                标题：%s

                时光流转，万物皆有章法。围绕「%s」这一主题，我们不妨从当下所处的环境与背景谈起。近年来， related 领域的变化有目共睹，它既带来了机遇，也提出了新的挑战。本文正是在这样的背景下展开思考与叙述的。

                其一，从整体格局来看，「%s」的核心在于把握关键环节。只有厘清内在逻辑，抓住主要矛盾，才能在复杂局面中找到正确的方向。实践一再证明，统筹兼顾、重点突破，是做好一切工作的基本方法。

                其二，从具体路径来看，需要做到三点：一是深入调研，摸清实情，让每一个判断都有数据与事实支撑；二是精准施策，针对不同的问题采取差异化的方案，避免一刀切；三是建立长效机制，把好的做法固化下来，形成可复制、可推广的经验。

                其三，从价值意义上来看，「%s」不仅关乎当下的成效，更关乎长远的积累。把眼前的事做扎实，把打基础的事做到位，时间的复利终会给出令人欣喜的回报。

                行而不辍，未来可期。总而言之，面对「%s」这一课题，我们既要有仰望格局的视野，也要有脚踏实地的耐心。相信只要方向明确、方法得当、执行有力，就一定能交出一份满意的答卷。

                （注：当前为本地模拟生成模式。在「系统管理-密钥管理」中配置大模型API密钥后，即可获得真实智能生成效果。）
                """.formatted(topic, topic, topic, topic, topic).replace(" related ", " 相关 ");
    }

    private String mockPolish(String prompt) {
        int idx = prompt.indexOf("【原文】");
        String original = idx >= 0 ? prompt.substring(idx + 4).trim() : prompt;
        StringBuilder sb = new StringBuilder();
        for (String line : original.split("\n")) {
            String l = line.stripTrailing();
            if (l.isBlank()) {
                sb.append("\n");
            } else if (l.startsWith("标题")) {
                sb.append(l).append("（润色版）\n");
            } else {
                sb.append(l.replaceAll("[，。]", "，")).append("。句式经过优化，语意更连贯，用词更凝练。\n");
            }
        }
        sb.append("\n（注：当前为本地模拟润色模式，配置API密钥后由大模型执行真实润色。）");
        return sb.toString();
    }

    private String mockRecommend() {
        return """
                · 可以从一个具体的场景或故事切入，用细节唤起读者共鸣，再过渡到主题论述。
                · 主体部分建议采用「观点+例证+分析」的结构，让每一段都有清晰的中心句。
                · 结尾可回扣标题并升华主旨，适当使用整散结合的句式增强节奏感。
                """;
    }

    private String extractBetween(String s, String a, String b) {
        int i = s.indexOf(a);
        if (i < 0) {
            return null;
        }
        int j = s.indexOf(b, i + a.length());
        return j > i ? s.substring(i + a.length(), j).trim() : null;
    }

    private String extractAfter(String s, String marker) {
        int i = s.indexOf(marker);
        if (i < 0) {
            return null;
        }
        String rest = s.substring(i + marker.length()).replace("】", "").strip();
        String first = rest.split("\n")[0].trim();
        return first.isEmpty() ? null : first;
    }
}

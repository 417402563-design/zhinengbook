package cn.edu.sdua.zhinengbook.service;

import cn.edu.sdua.zhinengbook.config.SafetyProperties;
import cn.edu.sdua.zhinengbook.exception.ApiException;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 内容安全过滤：基于DFA字典树的敏感词检测，
 * 同时应用于「用户输入提示词」与「模型输出内容」两道防线。
 */
@Service
@RequiredArgsConstructor
public class ContentSafetyService {

    private final SafetyProperties safetyProperties;

    /** DFA 根节点 */
    private final Map<Character, Object> root = new HashMap<>();

    @SuppressWarnings("unchecked")
    private static Map<Character, Object> child(Object o) {
        return (Map<Character, Object>) o;
    }

    @PostConstruct
    public void init() {
        if (safetyProperties.sensitiveWords() == null) {
            return;
        }
        for (String word : safetyProperties.sensitiveWords()) {
            addWord(word);
        }
    }

    private void addWord(String word) {
        Map<Character, Object> node = root;
        for (char c : word.toCharArray()) {
            node = child(node.computeIfAbsent(c, k -> new HashMap<Character, Object>()));
        }
        node.put('\0', Boolean.TRUE);
    }

    /** 检测命中的敏感词列表 */
    public List<String> detect(String text) {
        List<String> hits = new ArrayList<>();
        if (!safetyProperties.enabled() || text == null || text.isEmpty()) {
            return hits;
        }
        char[] chars = text.toCharArray();
        int i = 0;
        while (i < chars.length) {
            Map<Character, Object> node = root;
            int j = i;
            String last = null;
            while (j < chars.length && node.containsKey(chars[j])) {
                node = child(node.get(chars[j]));
                j++;
                if (Boolean.TRUE.equals(node.get('\0'))) {
                    last = text.substring(i, j);
                }
            }
            if (last != null) {
                hits.add(last);
                i = i + last.length();
            } else {
                i++;
            }
        }
        return hits;
    }

    /** 输入防线：命中敏感词直接拒绝 */
    public void assertInputSafe(String text) {
        List<String> hits = detect(text);
        if (!hits.isEmpty()) {
            throw new ApiException("输入内容包含违规词：" + String.join("、", hits) + "，请修改后重试");
        }
    }

    /** 输出防线：对模型生成内容进行脱敏替换 */
    public String sanitizeOutput(String text) {
        if (!safetyProperties.enabled() || text == null) {
            return text;
        }
        String result = text;
        String rep = safetyProperties.replacement() == null ? "**" : safetyProperties.replacement();
        for (String hit : detect(result)) {
            result = result.replace(hit, rep);
        }
        return result;
    }
}

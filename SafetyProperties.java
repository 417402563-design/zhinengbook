package cn.edu.sdua.zhinengbook.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * 内容安全过滤配置
 */
@ConfigurationProperties(prefix = "safety")
public record SafetyProperties(
        boolean enabled,
        List<String> sensitiveWords,
        String replacement
) {
}

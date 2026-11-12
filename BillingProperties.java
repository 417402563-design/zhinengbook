package cn.edu.sdua.zhinengbook.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 计费配置
 */
@ConfigurationProperties(prefix = "billing")
public record BillingProperties(
        double pricePer1kTokens,
        long freeTokensPerUser
) {
}

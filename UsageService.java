package cn.edu.sdua.zhinengbook.service;

import cn.edu.sdua.zhinengbook.config.BillingProperties;
import cn.edu.sdua.zhinengbook.entity.UsageRecord;
import cn.edu.sdua.zhinengbook.entity.User;
import cn.edu.sdua.zhinengbook.repository.UsageRepository;
import cn.edu.sdua.zhinengbook.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 用量统计与计费
 */
@Service
@RequiredArgsConstructor
public class UsageService {

    private final UsageRepository usageRepository;
    private final UserRepository userRepository;
    private final BillingProperties billingProperties;

    /** 记录一次大模型调用并计费（免费额度内不计费） */
    public UsageRecord record(Long userId, String operation, Long templateId, Long workId,
                              LlmClient.LlmResult result, String errorMsg) {
        UsageRecord rec = new UsageRecord();
        rec.setUserId(userId);
        rec.setOperation(operation);
        rec.setTemplateId(templateId);
        rec.setWorkId(workId);
        rec.setPromptTokens(result == null ? 0 : result.promptTokens());
        rec.setCompletionTokens(result == null ? 0 : result.completionTokens());
        rec.setTotalTokens(result == null ? 0 : result.totalTokens());
        rec.setRealCall(result != null && result.realCall());
        rec.setSuccess(errorMsg == null);
        rec.setErrorMsg(errorMsg);

        User user = userRepository.findById(userId).orElse(null);
        if (user != null && result != null) {
            long used = user.getTokensUsed();
            long free = billingProperties.freeTokensPerUser();
            int billable = (int) Math.max(0, Math.min(result.totalTokens(),
                    used + result.totalTokens() - free));
            rec.setCost(billable / 1000.0 * billingProperties.pricePer1kTokens());
            user.setTokensUsed(used + result.totalTokens());
            user.setCostIncurred(user.getCostIncurred() + rec.getCost());
            userRepository.save(user);
        }
        return usageRepository.save(rec);
    }

    /** 个人用量统计 */
    public Map<String, Object> myStats(Long userId) {
        List<UsageRecord> records = usageRepository.findByUserIdOrderByCreatedAtDesc(userId);
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("totalCalls", records.size());
        m.put("totalTokens", usageRepository.sumTokensByUser(userId));
        m.put("totalCost", usageRepository.sumCostByUser(userId));
        m.put("freeQuota", billingProperties.freeTokensPerUser());
        m.put("pricePer1kTokens", billingProperties.pricePer1kTokens());
        m.put("recent", records.stream().limit(50).map(this::toMap).toList());
        return m;
    }

    /** 管理员全局统计 */
    public Map<String, Object> adminStats() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("totalRecords", usageRepository.count());
        m.put("callsToday", usageRepository.countByCreatedAtAfter(
                LocalDateTime.now().withHour(0).withMinute(0).withSecond(0)));
        List<Map<String, Object>> byOp = new ArrayList<>();
        for (Object[] row : usageRepository.groupByOperation()) {
            byOp.add(Map.of("operation", row[0], "calls", row[1], "tokens", row[2], "cost", row[3]));
        }
        m.put("byOperation", byOp);
        List<Map<String, Object>> byUser = new ArrayList<>();
        for (Object[] row : usageRepository.groupByUser()) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("userId", row[0]);
            item.put("calls", row[1]);
            item.put("tokens", row[2]);
            item.put("cost", row[3]);
            item.put("username", userRepository.findById((Long) row[0]).map(User::getUsername).orElse("?"));
            byUser.add(item);
        }
        m.put("byUser", byUser);
        return m;
    }

    private Map<String, Object> toMap(UsageRecord r) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", r.getId());
        m.put("operation", r.getOperation());
        m.put("templateId", r.getTemplateId());
        m.put("workId", r.getWorkId());
        m.put("promptTokens", r.getPromptTokens());
        m.put("completionTokens", r.getCompletionTokens());
        m.put("totalTokens", r.getTotalTokens());
        m.put("cost", String.format("%.4f", r.getCost()));
        m.put("realCall", r.getRealCall());
        m.put("success", r.getSuccess());
        m.put("createdAt", r.getCreatedAt());
        return m;
    }
}

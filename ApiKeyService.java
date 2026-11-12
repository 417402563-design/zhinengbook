package cn.edu.sdua.zhinengbook.service;

import cn.edu.sdua.zhinengbook.dto.Dtos;
import cn.edu.sdua.zhinengbook.entity.ApiKeyConfig;
import cn.edu.sdua.zhinengbook.exception.ApiException;
import cn.edu.sdua.zhinengbook.repository.ApiKeyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * API密钥管理（仅管理员）
 */
@Service
@RequiredArgsConstructor
public class ApiKeyService {

    private final ApiKeyRepository apiKeyRepository;
    private final LlmClient llmClient;

    /** 列表（密钥脱敏） */
    public List<Map<String, Object>> listMasked() {
        return apiKeyRepository.findAllByOrderByIdAsc().stream().map(k -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", k.getId());
            m.put("name", k.getName());
            m.put("provider", k.getProvider());
            m.put("baseUrl", k.getBaseUrl());
            m.put("model", k.getModel());
            m.put("apiKeyMasked", mask(k.getApiKey()));
            m.put("active", k.getActive());
            m.put("status", k.getStatus());
            m.put("remark", k.getRemark());
            m.put("lastTestedAt", k.getLastTestedAt());
            return m;
        }).toList();
    }

    @Transactional
    public ApiKeyConfig save(Dtos.ApiKeyRequest req) {
        ApiKeyConfig k;
        if (req.id() != null) {
            k = apiKeyRepository.findById(req.id())
                    .orElseThrow(() -> new ApiException(404, "密钥配置不存在"));
        } else {
            k = new ApiKeyConfig();
        }
        k.setName(req.name());
        k.setProvider(req.provider());
        k.setBaseUrl(req.baseUrl());
        k.setModel(req.model());
        k.setRemark(req.remark());
        // 编辑时留空表示不修改原密钥
        if (req.apiKey() != null && !req.apiKey().isBlank()) {
            k.setApiKey(req.apiKey());
        }
        if (k.getApiKey() == null || k.getApiKey().isBlank()) {
            throw new ApiException("API密钥不能为空");
        }
        return apiKeyRepository.save(k);
    }

    /** 激活/停用切换：已激活的密钥再次调用则停用 */
    @Transactional
    public boolean toggleActive(Long id) {
        ApiKeyConfig target = apiKeyRepository.findById(id)
                .orElseThrow(() -> new ApiException(404, "密钥配置不存在"));
        boolean newActive = !Boolean.TRUE.equals(target.getActive());
        apiKeyRepository.findAll().forEach(k -> {
            k.setActive(newActive && k.getId().equals(id));
            apiKeyRepository.save(k);
        });
        return newActive;
    }

    public Map<String, Object> test(Long id) {
        ApiKeyConfig k = apiKeyRepository.findById(id)
                .orElseThrow(() -> new ApiException(404, "密钥配置不存在"));
        k.setLastTestedAt(LocalDateTime.now());
        try {
            LlmClient.LlmResult r = llmClient.testConnection(k.getBaseUrl(), k.getApiKey(), k.getModel());
            k.setStatus("OK");
            apiKeyRepository.save(k);
            return Map.of("success", true, "message", "连接成功", "reply", r.text());
        } catch (Exception e) {
            k.setStatus("FAILED");
            apiKeyRepository.save(k);
            return Map.of("success", false, "message", "连接失败：" + e.getMessage());
        }
    }

    @Transactional
    public void delete(Long id) {
        ApiKeyConfig k = apiKeyRepository.findById(id)
                .orElseThrow(() -> new ApiException(404, "密钥配置不存在"));
        if (Boolean.TRUE.equals(k.getActive())) {
            throw new ApiException("请先激活其他密钥再删除当前激活密钥");
        }
        apiKeyRepository.delete(k);
    }

    private String mask(String key) {
        if (key == null || key.length() <= 8) {
            return "****";
        }
        return key.substring(0, 4) + "****" + key.substring(key.length() - 4);
    }
}

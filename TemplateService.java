package cn.edu.sdua.zhinengbook.service;

import cn.edu.sdua.zhinengbook.dto.Dtos;
import cn.edu.sdua.zhinengbook.entity.WritingTemplate;
import cn.edu.sdua.zhinengbook.exception.ApiException;
import cn.edu.sdua.zhinengbook.repository.TemplateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * 写作模板管理与多场景选择
 */
@Service
@RequiredArgsConstructor
public class TemplateService {

    private final TemplateRepository templateRepository;

    public List<WritingTemplate> listEnabled() {
        return templateRepository.findByEnabledTrueOrderByUsageCountDesc();
    }

    public List<WritingTemplate> listAll() {
        return templateRepository.findAllByOrderByUsageCountDesc();
    }

    public WritingTemplate getEnabled(Long id) {
        WritingTemplate t = templateRepository.findById(id)
                .orElseThrow(() -> new ApiException(404, "模板不存在"));
        if (!Boolean.TRUE.equals(t.getEnabled())) {
            throw new ApiException("模板已停用");
        }
        return t;
    }

    public WritingTemplate save(Long id, Dtos.TemplateRequest req) {
        WritingTemplate t;
        if (id != null) {
            t = templateRepository.findById(id).orElseThrow(() -> new ApiException(404, "模板不存在"));
        } else {
            t = new WritingTemplate();
        }
        t.setName(req.name());
        t.setCategory(req.category());
        t.setDescription(req.description());
        t.setSystemPrompt(req.systemPrompt());
        t.setPromptTemplate(req.promptTemplate());
        t.setVariables(req.variables());
        if (req.enabled() != null) {
            t.setEnabled(req.enabled());
        }
        return templateRepository.save(t);
    }

    public void delete(Long id) {
        WritingTemplate t = templateRepository.findById(id)
                .orElseThrow(() -> new ApiException(404, "模板不存在"));
        if (Boolean.TRUE.equals(t.getBuiltin())) {
            throw new ApiException("系统内置模板不可删除，可停用");
        }
        templateRepository.delete(t);
    }

    public WritingTemplate toggle(Long id) {
        WritingTemplate t = templateRepository.findById(id)
                .orElseThrow(() -> new ApiException(404, "模板不存在"));
        t.setEnabled(!Boolean.TRUE.equals(t.getEnabled()));
        return templateRepository.save(t);
    }

    public void bumpUsage(Long id) {
        Optional<WritingTemplate> t = templateRepository.findById(id);
        t.ifPresent(x -> {
            x.setUsageCount(x.getUsageCount() + 1);
            templateRepository.save(x);
        });
    }
}

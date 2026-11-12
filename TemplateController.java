package cn.edu.sdua.zhinengbook.controller;

import cn.edu.sdua.zhinengbook.dto.Dtos;
import cn.edu.sdua.zhinengbook.entity.WritingTemplate;
import cn.edu.sdua.zhinengbook.service.TemplateService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class TemplateController {

    private final TemplateService templateService;

    /** 用户选择多场景写作模板 */
    @GetMapping("/templates")
    public Map<String, Object> list() {
        List<WritingTemplate> templates = templateService.listEnabled();
        return ApiResponse.ok(Map.of("templates", templates,
                "categories", templates.stream().map(WritingTemplate::getCategory).distinct().toList()));
    }

    // ========== 管理员模板管理 ==========

    @GetMapping("/admin/templates")
    public Map<String, Object> adminList() {
        return ApiResponse.ok(templateService.listAll());
    }

    @PostMapping("/admin/templates")
    public Map<String, Object> create(@Valid @RequestBody Dtos.TemplateRequest req) {
        return ApiResponse.ok(templateService.save(null, req));
    }

    @PutMapping("/admin/templates/{id}")
    public Map<String, Object> update(@PathVariable Long id, @Valid @RequestBody Dtos.TemplateRequest req) {
        return ApiResponse.ok(templateService.save(id, req));
    }

    @PatchMapping("/admin/templates/{id}/toggle")
    public Map<String, Object> toggle(@PathVariable Long id) {
        return ApiResponse.ok(templateService.toggle(id));
    }

    @DeleteMapping("/admin/templates/{id}")
    public Map<String, Object> delete(@PathVariable Long id) {
        templateService.delete(id);
        return ApiResponse.ok(null);
    }
}

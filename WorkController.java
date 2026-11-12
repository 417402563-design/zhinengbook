package cn.edu.sdua.zhinengbook.controller;

import cn.edu.sdua.zhinengbook.config.AuthInterceptor;
import cn.edu.sdua.zhinengbook.dto.Dtos;
import cn.edu.sdua.zhinengbook.service.WorkService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 作品管理 + 创作历史版本管理
 */
@RestController
@RequestMapping("/api/works")
@RequiredArgsConstructor
public class WorkController {

    private final WorkService workService;

    private Long uid(HttpServletRequest request) {
        return (Long) request.getAttribute(AuthInterceptor.ATTR_USER_ID);
    }

    @GetMapping
    public Map<String, Object> list(HttpServletRequest request) {
        return ApiResponse.ok(workService.listMine(uid(request)));
    }

    @GetMapping("/{id}")
    public Map<String, Object> detail(HttpServletRequest request, @PathVariable Long id) {
        return ApiResponse.ok(workService.detail(uid(request), id));
    }

    @PostMapping
    public Map<String, Object> save(HttpServletRequest request, @Valid @RequestBody Dtos.WorkSaveRequest req) {
        return ApiResponse.ok(Map.of("id", workService.save(uid(request), req).getId()));
    }

    @DeleteMapping("/{id}")
    public Map<String, Object> delete(HttpServletRequest request, @PathVariable Long id) {
        workService.delete(uid(request), id);
        return ApiResponse.ok(null);
    }

    @PatchMapping("/{id}/publish")
    public Map<String, Object> publish(HttpServletRequest request, @PathVariable Long id) {
        workService.publish(uid(request), id);
        return ApiResponse.ok(null);
    }

    // ========== 历史版本 ==========

    @GetMapping("/{id}/versions")
    public Map<String, Object> versions(HttpServletRequest request, @PathVariable Long id) {
        return ApiResponse.ok(workService.versions(uid(request), id));
    }

    @GetMapping("/{id}/versions/{versionNo}")
    public Map<String, Object> versionContent(HttpServletRequest request,
                                               @PathVariable Long id, @PathVariable Integer versionNo) {
        return ApiResponse.ok(Map.of("content", workService.versionContent(uid(request), id, versionNo)));
    }

    @PostMapping("/{id}/restore")
    public Map<String, Object> restore(HttpServletRequest request, @PathVariable Long id,
                                       @RequestBody Dtos.RestoreRequest req) {
        workService.restore(uid(request), id, req.versionNo());
        return ApiResponse.ok(null);
    }
}

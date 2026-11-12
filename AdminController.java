package cn.edu.sdua.zhinengbook.controller;

import cn.edu.sdua.zhinengbook.config.AuthInterceptor;
import cn.edu.sdua.zhinengbook.dto.Dtos;
import cn.edu.sdua.zhinengbook.service.ApiKeyService;
import cn.edu.sdua.zhinengbook.service.UsageService;
import cn.edu.sdua.zhinengbook.service.UserAdminService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 系统管理：API密钥管理、用量统计与计费、用户管理
 */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final ApiKeyService apiKeyService;
    private final UsageService usageService;
    private final UserAdminService userAdminService;

    private Long uid(HttpServletRequest request) {
        return (Long) request.getAttribute(AuthInterceptor.ATTR_USER_ID);
    }

    // ===== API密钥管理 =====

    @GetMapping("/apikeys")
    public Map<String, Object> listKeys() {
        return ApiResponse.ok(apiKeyService.listMasked());
    }

    @PostMapping("/apikeys")
    public Map<String, Object> saveKey(@Valid @RequestBody Dtos.ApiKeyRequest req) {
        return ApiResponse.ok(apiKeyService.save(req));
    }

    @DeleteMapping("/apikeys/{id}")
    public Map<String, Object> deleteKey(@PathVariable Long id) {
        apiKeyService.delete(id);
        return ApiResponse.ok(null);
    }

    @PatchMapping("/apikeys/{id}/activate")
    public Map<String, Object> activateKey(@PathVariable Long id) {
        boolean active = apiKeyService.toggleActive(id);
        return ApiResponse.ok(Map.of("active", active));
    }

    @PostMapping("/apikeys/{id}/test")
    public Map<String, Object> testKey(@PathVariable Long id) {
        return ApiResponse.ok(apiKeyService.test(id));
    }

    // ===== 用量统计与计费 =====

    @GetMapping("/usage/stats")
    public Map<String, Object> usageStats() {
        return ApiResponse.ok(usageService.adminStats());
    }

    // ===== 用户管理 =====

    @GetMapping("/users")
    public Map<String, Object> listUsers() {
        return ApiResponse.ok(userAdminService.listUsers());
    }

    @PatchMapping("/users/{id}/ban")
    public Map<String, Object> toggleBan(HttpServletRequest request, @PathVariable Long id) {
        return ApiResponse.ok(userAdminService.toggleBan(id, uid(request)));
    }

    @PatchMapping("/users/{id}/reset-password")
    public Map<String, Object> resetPassword(@PathVariable Long id, @RequestBody Map<String, String> body) {
        return ApiResponse.ok(userAdminService.resetPassword(id, body.get("newPassword")));
    }
}

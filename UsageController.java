package cn.edu.sdua.zhinengbook.controller;

import cn.edu.sdua.zhinengbook.config.AuthInterceptor;
import cn.edu.sdua.zhinengbook.service.UsageService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 用量统计（个人）
 */
@RestController
@RequestMapping("/api/usage")
@RequiredArgsConstructor
public class UsageController {

    private final UsageService usageService;

    @GetMapping("/me")
    public Map<String, Object> myStats(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute(AuthInterceptor.ATTR_USER_ID);
        return ApiResponse.ok(usageService.myStats(userId));
    }
}

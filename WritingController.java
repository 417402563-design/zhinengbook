package cn.edu.sdua.zhinengbook.controller;

import cn.edu.sdua.zhinengbook.config.AuthInterceptor;
import cn.edu.sdua.zhinengbook.dto.Dtos;
import cn.edu.sdua.zhinengbook.service.GenerationService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 核心写作功能：实时生成、文本润色与优化、实时推荐
 */
@RestController
@RequestMapping("/api/writing")
@RequiredArgsConstructor
public class WritingController {

    private final GenerationService generationService;

    private Long uid(HttpServletRequest request) {
        return (Long) request.getAttribute(AuthInterceptor.ATTR_USER_ID);
    }

    /** 基于模板 + 变量的AI生成 */
    @PostMapping("/generate")
    public Map<String, Object> generate(HttpServletRequest request, @RequestBody Dtos.GenerateRequest req) {
        return ApiResponse.ok(generationService.generate(uid(request), req));
    }

    /** 文本润色与优化 */
    @PostMapping("/polish")
    public Map<String, Object> polish(HttpServletRequest request, @Valid @RequestBody Dtos.PolishRequest req) {
        return ApiResponse.ok(generationService.polish(uid(request), req));
    }

    /** 实时写作推荐 */
    @PostMapping("/recommend")
    public Map<String, Object> recommend(HttpServletRequest request, @RequestBody Dtos.RecommendRequest req) {
        return ApiResponse.ok(generationService.recommend(uid(request), req));
    }
}

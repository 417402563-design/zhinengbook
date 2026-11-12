package cn.edu.sdua.zhinengbook.controller;

import cn.edu.sdua.zhinengbook.config.AuthInterceptor;
import cn.edu.sdua.zhinengbook.dto.Dtos;
import cn.edu.sdua.zhinengbook.service.AuthService;
import cn.edu.sdua.zhinengbook.service.UserAdminService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final UserAdminService userAdminService;

    @PostMapping("/register")
    public Map<String, Object> register(@Valid @RequestBody Dtos.RegisterRequest req) {
        return ApiResponse.ok(authService.register(req));
    }

    @PostMapping("/login")
    public Map<String, Object> login(@Valid @RequestBody Dtos.LoginRequest req) {
        return ApiResponse.ok(authService.login(req));
    }

    @PostMapping("/logout")
    public Map<String, Object> logout(@RequestHeader(value = "Authorization", required = false) String token) {
        authService.logout(token);
        return ApiResponse.ok(null);
    }

    @GetMapping("/me")
    public Map<String, Object> me(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute(AuthInterceptor.ATTR_USER_ID);
        return ApiResponse.ok(authService.profile(userAdminService.getById(userId)));
    }
}

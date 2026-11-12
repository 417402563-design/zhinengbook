package cn.edu.sdua.zhinengbook.config;

import cn.edu.sdua.zhinengbook.exception.ApiException;
import cn.edu.sdua.zhinengbook.service.TokenStore;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 登录认证与管理员权限拦截器
 */
@Component
@RequiredArgsConstructor
public class AuthInterceptor implements HandlerInterceptor {

    public static final String ATTR_USER_ID = "currentUserId";
    public static final String ATTR_USER_ROLE = "currentUserRole";
    public static final String ATTR_USERNAME = "currentUsername";

    private final TokenStore tokenStore;

    @Override
    public boolean preHandle(HttpServletRequest request, jakarta.servlet.http.HttpServletResponse response, Object handler) {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        String token = request.getHeader("Authorization");
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        TokenStore.Session session = tokenStore.get(token);
        if (session == null) {
            throw new ApiException(401, "未登录或登录已过期");
        }
        String uri = request.getRequestURI();
        if (uri.startsWith("/api/admin/") && !"ADMIN".equals(session.role())) {
            throw new ApiException(403, "需要管理员权限");
        }
        request.setAttribute(ATTR_USER_ID, session.userId());
        request.setAttribute(ATTR_USER_ROLE, session.role());
        request.setAttribute(ATTR_USERNAME, session.username());
        return true;
    }
}

package cn.edu.sdua.zhinengbook.service;

import cn.edu.sdua.zhinengbook.dto.Dtos;
import cn.edu.sdua.zhinengbook.entity.User;
import cn.edu.sdua.zhinengbook.exception.ApiException;
import cn.edu.sdua.zhinengbook.repository.UserRepository;
import cn.edu.sdua.zhinengbook.util.PasswordUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final TokenStore tokenStore;

    public Map<String, Object> register(Dtos.RegisterRequest req) {
        if (req.username() == null || req.username().length() < 3) {
            throw new ApiException("用户名至少3个字符");
        }
        if (req.password() == null || req.password().length() < 6) {
            throw new ApiException("密码至少6个字符");
        }
        if (userRepository.existsByUsername(req.username())) {
            throw new ApiException("用户名已存在");
        }
        User user = new User();
        user.setUsername(req.username());
        user.setPassword(PasswordUtil.hash(req.password()));
        user.setNickname(req.nickname() == null || req.nickname().isBlank() ? req.username() : req.nickname());
        userRepository.save(user);
        return login(new Dtos.LoginRequest(req.username(), req.password()));
    }

    public Map<String, Object> login(Dtos.LoginRequest req) {
        User user = userRepository.findByUsername(req.username() == null ? "" : req.username())
                .orElseThrow(() -> new ApiException("用户名或密码错误"));
        if (!PasswordUtil.matches(req.password(), user.getPassword())) {
            throw new ApiException("用户名或密码错误");
        }
        if ("BANNED".equals(user.getStatus())) {
            throw new ApiException("账号已被封禁，请联系管理员");
        }
        String token = tokenStore.create(user.getId(), user.getUsername(), user.getRole());
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("token", token);
        data.put("user", profile(user));
        return data;
    }

    public void logout(String token) {
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        tokenStore.remove(token);
    }

    public Map<String, Object> profile(User user) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", user.getId());
        m.put("username", user.getUsername());
        m.put("nickname", user.getNickname());
        m.put("role", user.getRole());
        m.put("tokensUsed", user.getTokensUsed());
        m.put("costIncurred", user.getCostIncurred());
        m.put("createdAt", user.getCreatedAt());
        return m;
    }
}

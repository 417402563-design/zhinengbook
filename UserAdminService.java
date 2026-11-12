package cn.edu.sdua.zhinengbook.service;

import cn.edu.sdua.zhinengbook.entity.User;
import cn.edu.sdua.zhinengbook.exception.ApiException;
import cn.edu.sdua.zhinengbook.repository.WorkRepository;
import cn.edu.sdua.zhinengbook.repository.UserRepository;
import cn.edu.sdua.zhinengbook.util.PasswordUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 用户管理（仅管理员）
 */
@Service
@RequiredArgsConstructor
public class UserAdminService {

    private final UserRepository userRepository;
    private final WorkRepository workRepository;

    public List<Map<String, Object>> listUsers() {
        return userRepository.findAll().stream().map(u -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", u.getId());
            m.put("username", u.getUsername());
            m.put("nickname", u.getNickname());
            m.put("role", u.getRole());
            m.put("status", u.getStatus());
            m.put("tokensUsed", u.getTokensUsed());
            m.put("costIncurred", u.getCostIncurred());
            m.put("workCount", workRepository.countByUserId(u.getId()));
            m.put("createdAt", u.getCreatedAt());
            return m;
        }).toList();
    }

    public User getById(Long id) {
        return userRepository.findById(id).orElseThrow(() -> new ApiException(404, "用户不存在"));
    }

    public Map<String, Object> toggleBan(Long id, Long currentUserId) {
        if (id.equals(currentUserId)) {
            throw new ApiException("不能封禁自己");
        }
        User u = getById(id);
        if ("ADMIN".equals(u.getRole())) {
            throw new ApiException("不能封禁管理员账号");
        }
        u.setStatus("NORMAL".equals(u.getStatus()) ? "BANNED" : "NORMAL");
        userRepository.save(u);
        return Map.of("id", u.getId(), "status", u.getStatus());
    }

    public Map<String, Object> resetPassword(Long id, String newPassword) {
        if (newPassword == null || newPassword.length() < 6) {
            throw new ApiException("新密码至少6个字符");
        }
        User u = getById(id);
        u.setPassword(PasswordUtil.hash(newPassword));
        userRepository.save(u);
        return Map.of("id", u.getId(), "message", "密码已重置");
    }
}

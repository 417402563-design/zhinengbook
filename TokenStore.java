package cn.edu.sdua.zhinengbook.service;

import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 内存Token会话存储
 */
@Component
public class TokenStore {

    public record Session(Long userId, String username, String role, LocalDateTime expiredAt) {
    }

    private static final long EXPIRE_HOURS = 24;

    private final Map<String, Session> sessions = new ConcurrentHashMap<>();

    public String create(Long userId, String username, String role) {
        String token = UUID.randomUUID().toString().replace("-", "");
        sessions.put(token, new Session(userId, username, role,
                LocalDateTime.now().plusHours(EXPIRE_HOURS)));
        return token;
    }

    public Session get(String token) {
        if (token == null) {
            return null;
        }
        Session s = sessions.get(token);
        if (s != null && s.expiredAt().isBefore(LocalDateTime.now())) {
            sessions.remove(token);
            return null;
        }
        return s;
    }

    public void remove(String token) {
        sessions.remove(token);
    }
}

package cn.edu.sdua.zhinengbook.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * SHA-256 加盐口令散列（演示用途，生产建议替换为BCrypt）
 */
public final class PasswordUtil {

    private static final String SALT = "zhinengbook$2026$";

    private PasswordUtil() {
    }

    public static String hash(String raw) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest((SALT + raw).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    public static boolean matches(String raw, String hashed) {
        return hash(raw).equals(hashed);
    }
}

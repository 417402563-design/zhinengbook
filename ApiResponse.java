package cn.edu.sdua.zhinengbook.controller;

import java.util.Map;

/**
 * 统一API响应体
 */
public final class ApiResponse {

    private ApiResponse() {
    }

    public static Map<String, Object> ok(Object data) {
        return Map.of("code", 0, "message", "ok", "data", data == null ? Map.of() : data);
    }
}

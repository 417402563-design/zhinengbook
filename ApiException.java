package cn.edu.sdua.zhinengbook.exception;

/**
 * 业务异常
 */
public class ApiException extends RuntimeException {

    private final int code;

    public ApiException(String message) {
        this(400, message);
    }

    public ApiException(int code, String message) {
        super(message);
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}

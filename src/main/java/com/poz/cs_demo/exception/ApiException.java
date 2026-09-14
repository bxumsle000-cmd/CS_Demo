package com.poz.cs_demo.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * 業務邏輯例外。
 * <p>
 * 在 Service 層遇到「預期中的錯誤」（例如找不到資料、帳號密碼錯誤、權限不足）時拋出，
 * 由 {@link GlobalExceptionHandler} 統一轉成對應的 HTTP 狀態碼與 JSON 回應。
 * <p>
 * 用法範例：
 * <pre>
 *   throw new ApiException(HttpStatus.NOT_FOUND, "找不到工單：" + ticketId);
 *   throw ApiException.notFound("找不到客服");
 * </pre>
 */
@Getter
public class ApiException extends RuntimeException {

    /** 要回給前端的 HTTP 狀態碼 */
    private final HttpStatus status;

    public ApiException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    // ---- 常用的快捷建構方法，讓 Service 端寫起來更短 ----

    /** 400：請求內容有問題 */
    public static ApiException badRequest(String message) {
        return new ApiException(HttpStatus.BAD_REQUEST, message);
    }

    /** 401：未登入或登入資訊無效 */
    public static ApiException unauthorized(String message) {
        return new ApiException(HttpStatus.UNAUTHORIZED, message);
    }

    /** 403：已登入但沒有權限 */
    public static ApiException forbidden(String message) {
        return new ApiException(HttpStatus.FORBIDDEN, message);
    }

    /** 404：找不到資料 */
    public static ApiException notFound(String message) {
        return new ApiException(HttpStatus.NOT_FOUND, message);
    }

    /** 409：資料衝突（例如帳號已存在） */
    public static ApiException conflict(String message) {
        return new ApiException(HttpStatus.CONFLICT, message);
    }
}

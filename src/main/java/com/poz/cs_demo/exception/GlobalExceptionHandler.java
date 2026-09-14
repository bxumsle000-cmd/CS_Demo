package com.poz.cs_demo.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

/**
 * 全域例外處理器。
 * <p>
 * 任何 Controller 拋出的例外都會被這裡攔截，轉成統一的 {@link ErrorResponse} JSON 回應，
 * Controller 本身不需要寫 try/catch。
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /** 業務邏輯例外：狀態碼與訊息直接取自 ApiException */
    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ErrorResponse> handleApiException(ApiException e) {
        log.warn("ApiException [{}] {}", e.getStatus().value(), e.getMessage());
        return ResponseEntity
                .status(e.getStatus())
                .body(new ErrorResponse(e.getStatus().value(), e.getMessage()));
    }

    /** @Valid 驗證失敗：把每個欄位的錯誤訊息合併成一句話回傳 */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException e) {
        String message = e.getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .collect(Collectors.joining("; "));
        log.warn("驗證失敗 {}", message);
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(HttpStatus.BAD_REQUEST.value(), message));
    }

    /** 其他未預期的例外：回 500，並把完整堆疊印到 log 方便除錯，但不把細節洩漏給前端 */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception e) {
        log.error("未預期的錯誤", e);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR.value(), "系統發生錯誤，請稍後再試"));
    }
}

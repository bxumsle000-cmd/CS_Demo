package com.poz.cs_demo.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.StringJoiner;

/**
 * 全域例外處理器。
 * <p>
 * 任何 Controller 拋出的例外都會被這裡攔截，轉成統一的 {@link ErrorResponse} JSON 回應，
 * Controller 本身不需要寫 try/catch。
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 【1. 業務邏輯例外】負責處理程式「預期中」的錯誤。
     * <p>
     * 觸發時機：Service 主動 throw ApiException，例如
     * 找不到資料（404）、帳號密碼錯誤（401）、沒有權限（403）、資料重複（409）。
     * <p>
     * 處理方式：狀態碼與訊息都直接取自 ApiException，原樣回給前端。
     * 這類訊息是我們自己寫的、給使用者看的，所以可以放心顯示。
     * <p>
     * 例：throw ApiException.notFound("找不到工單：T001")
     *   → HTTP 404 { "code": 404, "message": "找不到工單：T001" }
     */
    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ErrorResponse> handleApiException(ApiException e) {
        log.warn("ApiException [{}] {}", e.getStatus().value(), e.getMessage());
        return ResponseEntity
                .status(e.getStatus())
                .body(new ErrorResponse(e.getStatus().value(), e.getMessage()));
    }

    /**
     * 【2. 請求驗證失敗】負責處理前端送來的資料「格式不對」。
     * <p>
     * 觸發時機：Controller 參數加了 @Valid，而 DTO 欄位上的
     * @NotBlank、@Size、@Email 等規則沒通過時，Spring 會自動丟出
     * MethodArgumentNotValidException，不需要我們自己 throw。
     * <p>
     * 處理方式：固定回 400，並把每個欄位的錯誤訊息接成一句話，
     * 讓前端知道是哪些欄位、各自錯在哪。
     * <p>
     * 例：name 空白、email 格式錯
     *   → HTTP 400 { "code": 400, "message": "name: 不可為空; email: 格式錯誤" }
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException e) {
        // 把每個欄位錯誤組成 "欄位: 訊息"，再用 "; " 接成一句
        StringJoiner joiner = new StringJoiner("; ");
        for (FieldError fe : e.getFieldErrors()) {
            joiner.add(fe.getField() + ": " + fe.getDefaultMessage());
        }
        String message = joiner.toString();
        log.warn("驗證失敗 {}", message);
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(HttpStatus.BAD_REQUEST.value(), message));
    }

    /**
     * 【3. 未預期的例外（兜底）】負責處理上面兩個沒接到的「所有其他錯誤」。
     * <p>
     * 觸發時機：程式 bug（NullPointerException 等）、資料庫連不上、
     * 第三方服務失敗……任何我們沒料到、也沒有主動 throw 的例外。
     * Exception 是所有例外的父類別，所以這裡等於「其他全部」；
     * 但 Spring 會優先挑型別最精確的 handler，ApiException 不會掉到這裡。
     * <p>
     * 處理方式：
     * <ul>
     *   <li>log.error 帶入整個例外物件 → 完整 stack trace 印到伺服器 log，方便我們追查。</li>
     *   <li>回給前端固定 500 + 一句籠統訊息，不用 e.getMessage()，
     *       避免把資料庫位置、內部路徑等細節洩漏出去。</li>
     * </ul>
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception e) {
        log.error("未預期的錯誤", e);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR.value(), "系統發生錯誤，請稍後再試"));
    }
}

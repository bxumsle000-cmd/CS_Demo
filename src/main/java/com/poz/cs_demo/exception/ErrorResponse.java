package com.poz.cs_demo.exception;

/**
 * 統一的錯誤回應格式，所有錯誤都會轉成這個 JSON 回給前端。
 * <pre>
 * {
 *   "code": 404,
 *   "message": "找不到工單：T001"
 * }
 * </pre>
 *
 * @param code    HTTP 狀態碼
 * @param message 給前端看的錯誤訊息
 */
public record ErrorResponse(int code, String message) {
}

package org.lifelab.lifelabbe.common;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /** GlobalException (ErrorCode 기반) */
    @ExceptionHandler(GlobalException.class)
    public ResponseEntity<ApiResponse<Void>> handleGlobal(GlobalException e) {
        ErrorCode code = e.getErrorCode();
        log.error("GlobalException 발생", e);

        return ResponseEntity
                .status(code.status())
                .body(ApiResponse.fail(code));
    }

    /** 400 - 검증 실패 */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException e) {
        log.error("Validation failed", e);

        return ResponseEntity
                .status(ErrorCode.INVALID_PARAMETER.status())
                .body(ApiResponse.fail(ErrorCode.INVALID_PARAMETER));
    }

    /** 400 - 잘못된 파라미터 */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> handleBadRequest(IllegalArgumentException e) {
        log.error("Invalid parameter", e);

        return ResponseEntity
                .status(ErrorCode.INVALID_PARAMETER.status())
                .body(ApiResponse.fail(ErrorCode.INVALID_PARAMETER));
    }

    /** 403 - 권한 없음 */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleForbidden(AccessDeniedException e) {
        log.error("Access denied", e);

        return ResponseEntity
                .status(ErrorCode.FORBIDDEN.status())
                .body(ApiResponse.fail(ErrorCode.FORBIDDEN));
    }

    /** 500 - 서버 에러 */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleServerError(Exception e) {
        log.error("Server internal error", e);

        return ResponseEntity
                .status(ErrorCode.SERVER_500.status())
                .body(ApiResponse.fail(ErrorCode.SERVER_500));
    }
}
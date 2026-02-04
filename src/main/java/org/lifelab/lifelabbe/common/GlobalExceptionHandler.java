package org.lifelab.lifelabbe.common;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    /** 400 - 검증 실패*/
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException e) {
        return ResponseEntity
                .status(ErrorCode.INVALID_PARAMETER.status())
                .body(ApiResponse.fail(ErrorCode.INVALID_PARAMETER));
    }

    /** 400 - 잘못된 파라미터 */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> handleBadRequest(IllegalArgumentException e) {
        return ResponseEntity
                .status(ErrorCode.INVALID_PARAMETER.status())
                .body(ApiResponse.fail(ErrorCode.INVALID_PARAMETER));
    }

    /** 403 - 권한 없음 */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleForbidden(AccessDeniedException e) {
        return ResponseEntity
                .status(ErrorCode.FORBIDDEN.status())
                .body(ApiResponse.fail(ErrorCode.FORBIDDEN));
    }

    /** 500 - 서비스에러 */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleServerError(Exception e) {
        return ResponseEntity
                .status(ErrorCode.SERVER_500.status())
                .body(ApiResponse.fail(ErrorCode.SERVER_500));
    }
}

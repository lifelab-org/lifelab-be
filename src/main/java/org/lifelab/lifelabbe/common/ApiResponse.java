package org.lifelab.lifelabbe.common;

import lombok.Builder;

@Builder
public record ApiResponse<T>(
        String result,
        int status,
        T success,
        ApiError error
) {
    /** 성공 응답 */
    public static ApiResponse<MessageBody> success(int status, String message) {
        return ApiResponse.<MessageBody>builder()
                .result("Success")
                .status(status)
                .success(new MessageBody(message))
                .error(null)
                .build();
    }

    /** 실패 응답 */
    public static ApiResponse<Void> fail(ErrorCode errorCode) {
        return ApiResponse.<Void>builder()
                .result("Fail")
                .status(errorCode.status())
                .success(null)
                .error(new ApiError(errorCode.code(), errorCode.message()))
                .build();
    }

    public record ApiError(String errorCode, String message) {}
    public record MessageBody(String message) {}
}

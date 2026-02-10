package org.lifelab.lifelabbe.common;

import lombok.Getter;

/**
 * 서비스/도메인 계층에서 ErrorCode를 담아 던지는 예외
 */
@Getter
public class GlobalException extends RuntimeException {

    private final ErrorCode errorCode;

    public GlobalException(ErrorCode errorCode) {
        super(errorCode.message());
        this.errorCode = errorCode;
    }
    // 상세 메시지 포함 생성자
    public GlobalException(ErrorCode errorCode, String detailMessage) {
        super(detailMessage);
        this.errorCode = errorCode;
    }
}

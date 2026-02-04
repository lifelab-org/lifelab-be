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
}

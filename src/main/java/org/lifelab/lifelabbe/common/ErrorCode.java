package org.lifelab.lifelabbe.common;

public enum ErrorCode {

    /* 400 */
    INVALID_PARAMETER(400, "INVALID_PARAMETER", "요청 값이 올바르지 않습니다."),

    /* 401 */
    AUTH_401(401, "AUTH_401", "인증 토큰이 유효하지 않습니다."),

    /*  403  */
    FORBIDDEN(403, "FORBIDDEN", "해당 실험에 접근할 권한이 없습니다."),

    /*  404  */
    EXP_404(404, "EXP_404", "해당 실험을 찾을 수 없습니다."),

    /*  409  */
    REC_409(409, "REC_409", "이미 오늘의 기록이 존재합니다."),

    /*  500  */
    SERVER_500(500, "SERVER_500", "서버 내부 오류가 발생했습니다.");

    private final int status;
    private final String code;
    private final String message;

    ErrorCode(int status, String code, String message) {
        this.status = status;
        this.code = code;
        this.message = message;
    }

    public int status() {
        return status;
    }

    public String code() {
        return code;
    }

    public String message() {
        return message;
    }
}

package org.lifelab.lifelabbe.common;

public enum ErrorCode {

    /* 400 */
    INVALID_PARAMETER(400, "INVALID_PARAMETER", "요청 값이 올바르지 않습니다."),
    INVALID_START_DATE(400, "INVALID_START_DATE", "startDate는 오늘 이전으로 설정할 수 없습니다."),
    INVALID_DATE_RANGE(400, "INVALID_DATE_RANGE", "startDate는 endDate보다 늦을 수 없습니다."),
    DUPLICATE_RECORD_ITEM(400, "DUPLICATE_RECORD_ITEM", "recordItems.name이 중복되었습니다."),
    TOO_MANY_RECORD_ITEMS(400, "TOO_MANY_RECORD_ITEMS", "recordItems는 최대 10개까지 가능합니다."),
    PRE_STATE_ITEMS_MISMATCH(400, "PRE_STATE_ITEMS_MISMATCH", "실험의 기록 항목과 요청 항목이 일치하지 않습니다."),

    /* 401 */
    AUTH_401(401, "AUTH_401", "인증 토큰이 유효하지 않습니다."),

    /*  403  */
    FORBIDDEN(403, "FORBIDDEN", "해당 실험에 접근할 권한이 없습니다."),
    KAKAO_COOKIE_EXPIRED(403, "KAKAO_COOKIE_EXPIRED", "로그인이 만료되었습니다. 다시 로그인해주세요."),
    /*  404  */
    EXP_404(404, "EXP_404", "해당 실험을 찾을 수 없습니다."),
    PRE_STATE_404(404, "PRE_STATE_404", "실험 전 상태가 존재하지 않습니다."),
    /*  409  */
    REC_409(409, "REC_409", "이미 오늘의 기록이 존재합니다."),
    PRE_STATE_409(409, "PRE_STATE_409", "이미 실험 전 상태가 존재합니다."),

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

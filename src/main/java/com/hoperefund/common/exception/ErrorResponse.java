package com.hoperefund.common.exception;

/** 오류 응답 모양: {"error": "BAD_REQUEST", "message": "화면에 보여 줄 문구"} (계산 결과 오류 모양과 같음) */
public record ErrorResponse(String error, String message) {
}

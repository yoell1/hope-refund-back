package com.hoperefund.common.exception;

/** 오류 응답 모양: {"error": "VALIDATION", "message": "화면에 보여 줄 문구"} (모든 API 공통 · docs/api-rules.md) */
public record ErrorResponse(String error, String message) {
}

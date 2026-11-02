package com.hoperefund.common.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * 모든 API의 오류를 {"error", "message"} 모양으로 바꿔요.
 * @RestControllerAdvice: 모든 컨트롤러에서 던진 예외를 여기서 한 번에 받아요
 *
 * 보안: 오류 원인(스택 트레이스 · DB 주소 · SQL)은 서버 로그에만 남기고 응답에는 절대 넣지 않아요.
 */
@RestControllerAdvice
public class GlobalExceptionHandler{

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ErrorResponse> badRequest(BadRequestException e){
        return ResponseEntity.badRequest().body(new ErrorResponse("BAD_REQUEST", e.getMessage()));
    }

    /** 기준값이 없는 연도 (예: 2024): error 값을 "NO_CRITERIA"로 따로 보내요 (BadRequestException보다 먼저 잡혀요) */
    @ExceptionHandler(NoCriteriaException.class)
    public ResponseEntity<ErrorResponse> noCriteria(NoCriteriaException e){
        return ResponseEntity.badRequest().body(new ErrorResponse("NO_CRITERIA", e.getMessage()));
    }

    /** 계산 서버에 연결할 수 없을 때: 원인은 로그에만, 화면에는 다시 시도 안내 */
    @ExceptionHandler(CalcUnavailableException.class)
    public ResponseEntity<ErrorResponse> calcUnavailable(CalcUnavailableException e){
        log.error("계산 서버 연결 실패", e);
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(new ErrorResponse("CALC_UNAVAILABLE", "계산 서버에 잠시 연결할 수 없어요. 잠시 후 다시 시도해 주세요"));
    }

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ErrorResponse> notFound(NotFoundException e){
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorResponse("NOT_FOUND", e.getMessage()));
    }

    /** @Valid 검사 실패: 첫 번째 칸의 안내 문구를 보여줘요. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> invalid(MethodArgumentNotValidException e){
        FieldError first = e.getBindingResult().getFieldError();
        String message = first != null ? first.getDefaultMessage() : "입력값을 확인해 주세요";
        return ResponseEntity.badRequest().body(new ErrorResponse("VALIDATION", message));
    }

    /** JSON 형식이 틀렸을 때 (예: 숫자 칸에 글자) */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> unreadable(HttpMessageNotReadableException e){
        return ResponseEntity.badRequest().body(new ErrorResponse("VALIDATION", "입력 형식이 올바르지 않아요"));
    }

    /** 주소의 연도나 회원번호 헤더가 숫자가 아닐 때 (예: /api/incomes/abc) */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> typeMismatch(MethodArgumentTypeMismatchException e){
        return ResponseEntity.badRequest().body(new ErrorResponse("VALIDATION", "요청 값 형식이 올바르지 않아요"));
    }

    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<ErrorResponse> noMember(MissingRequestHeaderException e){
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ErrorResponse("UNAUTHORIZED", "로그인이 필요해요"));
    }

    /** 없는 주소 (예: /api/income) */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> noResource(NoResourceFoundException e){
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorResponse("NOT_FOUND", "요청한 주소가 없어요"));
    }

    /** 허용하지 않는 방식 (예: 계산 주소에 GET) */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> methodNotAllowed(HttpRequestMethodNotSupportedException e){
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(new ErrorResponse("METHOD_NOT_ALLOWED", "지원하지 않는 요청 방식이에요"));
    }

    /** 그 밖의 모든 오류 (DB 접속 실패 등): 원인은 로그에만, 응답은 짧은 안내만 */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> unexpected(Exception e){
        log.error("처리하지 못한 오류", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("SERVER_ERROR", "잠시 후 다시 시도해 주세요"));
    }
}

package kg.alatoo.payment.boot;

import kg.alatoo.payment.exceptions.DomainException;
import kg.alatoo.payment.exceptions.ErrorCode;
import kg.alatoo.payment.exceptions.PaymentFailedException;
import kg.alatoo.payment.exceptions.RefundFailedException;
import kg.alatoo.payment.rest.ErrorResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.List;

/**
 * Переводит исключения в единый формат ошибки API и выбирает HTTP-статус по коду ошибки.
 * Только для @RestController — Thymeleaf-страницы обрабатывают ошибки сами.
 */
@Slf4j
@RestControllerAdvice(annotations = RestController.class)
public class GlobalExceptionHandler {

    @ExceptionHandler(PaymentFailedException.class)
    public ResponseEntity<ErrorResponse> handlePaymentFailed(PaymentFailedException e) {
        return ResponseEntity.status(httpStatus(e.getCode()))
                .body(new ErrorResponse(e.getCode().name(), e.getMessage(), e.getPaymentId(), null, "FAILED", null));
    }

    @ExceptionHandler(RefundFailedException.class)
    public ResponseEntity<ErrorResponse> handleRefundFailed(RefundFailedException e) {
        return ResponseEntity.status(httpStatus(e.getCode()))
                .body(new ErrorResponse(e.getCode().name(), e.getMessage(), e.getPaymentId(), e.getRefundId(),
                        "FAILED", null));
    }

    @ExceptionHandler(DomainException.class)
    public ResponseEntity<ErrorResponse> handleDomain(DomainException e) {
        HttpStatus status = httpStatus(e.getCode());
        if (status.is5xxServerError()) {
            log.warn("{}: {}", e.getCode(), e.getMessage());
        }
        return ResponseEntity.status(status).body(ErrorResponse.of(e.getCode().name(), e.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException e) {
        List<ErrorResponse.FieldError> details = e.getBindingResult().getFieldErrors().stream()
                .map(f -> new ErrorResponse.FieldError(f.getField(), f.getDefaultMessage()))
                .toList();
        return ResponseEntity.badRequest()
                .body(new ErrorResponse(ErrorCode.VALIDATION_ERROR.name(), "Request validation failed",
                        null, null, null, details));
    }

    @ExceptionHandler({MissingRequestHeaderException.class, MissingServletRequestParameterException.class,
            MethodArgumentTypeMismatchException.class})
    public ResponseEntity<ErrorResponse> handleBadRequest(Exception e) {
        return ResponseEntity.badRequest().body(ErrorResponse.of(ErrorCode.VALIDATION_ERROR.name(), e.getMessage()));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleUnreadable(HttpMessageNotReadableException e) {
        return ResponseEntity.badRequest()
                .body(ErrorResponse.of(ErrorCode.VALIDATION_ERROR.name(), "Malformed JSON request body"));
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMethod(HttpRequestMethodNotSupportedException e) {
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                .body(ErrorResponse.of(ErrorCode.VALIDATION_ERROR.name(), e.getMessage()));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(NoResourceFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ErrorResponse.of("NOT_FOUND", e.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception e) {
        log.error("Unexpected error", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ErrorResponse.of(ErrorCode.INTERNAL_ERROR.name(), "Internal server error"));
    }

    /** Соответствие кода ошибки и HTTP-статуса. */
    static HttpStatus httpStatus(ErrorCode code) {
        return switch (code) {
            case UNAUTHORIZED -> HttpStatus.UNAUTHORIZED;
            case VALIDATION_ERROR, INVALID_AMOUNT -> HttpStatus.BAD_REQUEST;
            case CLIENT_NOT_FOUND, PAYMENT_NOT_FOUND -> HttpStatus.NOT_FOUND;
            case CLIENT_ALREADY_EXISTS, INVALID_STATE, IDEMPOTENCY_CONFLICT -> HttpStatus.CONFLICT;
            case WALLET_NOT_FOUND, CLIENT_BLOCKED, INSUFFICIENT_FUNDS, TREASURY_INSUFFICIENT_FUNDS,
                 REFUND_EXCEEDS_PAYMENT, CONFIRMATION_TIMEOUT -> HttpStatus.UNPROCESSABLE_ENTITY;
            case CLIENT_DATA_UNAVAILABLE -> HttpStatus.SERVICE_UNAVAILABLE;
            case BLOCKCHAIN_ERROR -> HttpStatus.BAD_GATEWAY;
            case INTERNAL_ERROR -> HttpStatus.INTERNAL_SERVER_ERROR;
        };
    }
}

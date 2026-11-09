package kg.alatoo.payment.rest;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;
import java.util.UUID;

/**
 * Единый формат ошибки API:
 * <pre>{ "code": "INSUFFICIENT_FUNDS", "message": "...", "paymentId": "...", "status": "FAILED" }</pre>
 * paymentId / refundId и status есть только если платёж (возврат) был создан и завершился ошибкой.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(
        String code,
        String message,
        UUID paymentId,
        UUID refundId,
        String status,
        List<FieldError> details
) {

    public static ErrorResponse of(String code, String message) {
        return new ErrorResponse(code, message, null, null, null, null);
    }

    public record FieldError(String field, String message) {
    }
}

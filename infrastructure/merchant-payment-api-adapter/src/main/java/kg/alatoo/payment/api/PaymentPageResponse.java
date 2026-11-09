package kg.alatoo.payment.api;

import java.util.List;

public record PaymentPageResponse(
        List<PaymentResponse> items,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
}

package kg.alatoo.payment.api;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record CreatePaymentRequest(
        @NotBlank @Size(max = 128)
        @Schema(description = "Внешний id клиента", example = "ext-42")
        String clientId,
        @NotNull
        @Schema(description = "Сумма в ETH (строкой, чтобы не терять точность)", type = "string", example = "0.001")
        BigDecimal amount
) {
}

package kg.alatoo.payment.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import kg.alatoo.payment.BlockchainExplorer;
import kg.alatoo.payment.CreatePaymentCommand;
import kg.alatoo.payment.CreatePaymentPort;
import kg.alatoo.payment.GetPaymentPort;
import kg.alatoo.payment.ListPaymentsPort;
import kg.alatoo.payment.ListPaymentsQuery;
import kg.alatoo.payment.auth.CurrentMerchant;
import kg.alatoo.payment.models.Merchant;
import kg.alatoo.payment.models.PageResult;
import kg.alatoo.payment.models.Payment;
import kg.alatoo.payment.models.PaymentStatus;
import kg.alatoo.payment.rest.Headers;
import kg.alatoo.payment.rest.OpenApiConfiguration;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Мерчантский API. Контроллер только переводит HTTP в команды use case и обратно —
 * вся бизнес-логика в application-слое.
 */
@RestController
@RequestMapping("/api/v1/payments")
@Tag(name = "Payments", description = "Мерчантский API платежей")
@SecurityRequirement(name = OpenApiConfiguration.MERCHANT_API_KEY)
public class PaymentController {
    private final CreatePaymentPort createPayment;
    private final GetPaymentPort getPayment;
    private final ListPaymentsPort listPayments;
    private final BlockchainExplorer explorer;

    public PaymentController(CreatePaymentPort createPayment,
                             GetPaymentPort getPayment,
                             ListPaymentsPort listPayments,
                             BlockchainExplorer explorer) {
        this.createPayment = createPayment;
        this.getPayment = getPayment;
        this.listPayments = listPayments;
        this.explorer = explorer;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Создать оплату",
            description = "Проверяет клиента, его данные в ABC и баланс, отправляет транзакцию и возвращает PENDING. "
                    + "Повтор с тем же Idempotency-Key и тем же телом возвращает тот же результат.")
    public PaymentResponse create(@Parameter(hidden = true) @CurrentMerchant Merchant merchant,
                                  @RequestHeader(Headers.IDEMPOTENCY_KEY) String idempotencyKey,
                                  @RequestBody @Valid CreatePaymentRequest request) {
        Payment payment = createPayment.execute(
                new CreatePaymentCommand(merchant, request.clientId(), request.amount(), idempotencyKey));
        return PaymentResponse.from(payment, explorer);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Статус платежа")
    public PaymentResponse get(@Parameter(hidden = true) @CurrentMerchant Merchant merchant,
                               @PathVariable UUID id) {
        return PaymentResponse.from(getPayment.execute(merchant.id(), id), explorer);
    }

    @GetMapping
    @Operation(summary = "Список платежей мерчанта")
    public PaymentPageResponse list(@Parameter(hidden = true) @CurrentMerchant Merchant merchant,
                                    @RequestParam(required = false) PaymentStatus status,
                                    @RequestParam(defaultValue = "0") int page,
                                    @RequestParam(defaultValue = "20") int size) {
        PageResult<Payment> result = listPayments.execute(new ListPaymentsQuery(merchant.id(), status, page, size));
        return new PaymentPageResponse(
                result.items().stream().map(p -> PaymentResponse.from(p, explorer)).toList(),
                result.page(), result.size(), result.totalElements(), result.totalPages());
    }
}

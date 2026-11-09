package kg.alatoo.payment.web;

import kg.alatoo.payment.AdminListPaymentsPort;
import kg.alatoo.payment.GetPaymentDetailsPort;
import kg.alatoo.payment.PaymentDetails;
import kg.alatoo.payment.RefundPaymentCommand;
import kg.alatoo.payment.RefundPaymentPort;
import kg.alatoo.payment.exceptions.DomainException;
import kg.alatoo.payment.models.PaymentStatus;
import kg.alatoo.payment.models.Refund;
import kg.alatoo.payment.models.RefundStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.UUID;

/**
 * Платежи: таблица с фильтром по статусу, страница платежа (история, возвраты) и кнопка «Вернуть».
 * Возврат идёт через тот же RefundPaymentPort, что и мерчантский REST API, от имени мерчанта платежа.
 */
@Controller
@RequestMapping("/admin/payments")
public class AdminPaymentsController {
    private final AdminListPaymentsPort listPayments;
    private final GetPaymentDetailsPort getPaymentDetails;
    private final RefundPaymentPort refundPayment;

    public AdminPaymentsController(AdminListPaymentsPort listPayments,
                                   GetPaymentDetailsPort getPaymentDetails,
                                   RefundPaymentPort refundPayment) {
        this.listPayments = listPayments;
        this.getPaymentDetails = getPaymentDetails;
        this.refundPayment = refundPayment;
    }

    @GetMapping
    public String list(@RequestParam(required = false) PaymentStatus status,
                       @RequestParam(defaultValue = "0") int page,
                       Model model) {
        model.addAttribute("result", listPayments.execute(status, page, 20));
        model.addAttribute("statuses", PaymentStatus.values());
        model.addAttribute("selectedStatus", status);
        model.addAttribute("active", "payments");
        return "admin/payments";
    }

    @GetMapping("/{id}")
    public String details(@PathVariable UUID id, Model model) {
        PaymentDetails details = getPaymentDetails.execute(id);
        model.addAttribute("details", details);
        // Пока есть неподтверждённые транзакции, страница сама обновляется — видно, как планировщик меняет статус
        model.addAttribute("autoRefresh", details.payment().status() == PaymentStatus.PENDING
                || details.refunds().stream().anyMatch(r -> r.status() == RefundStatus.PENDING));
        // Новый ключ идемпотентности на каждый показ формы: двойной клик по «Вернуть» не создаст два возврата
        model.addAttribute("refundKey", UUID.randomUUID().toString());
        model.addAttribute("active", "payments");
        return "admin/payment";
    }

    @PostMapping("/{id}/refunds")
    public String refund(@PathVariable UUID id,
                         @RequestParam(required = false) String amount,
                         @RequestParam String idempotencyKey,
                         RedirectAttributes redirect) {
        try {
            PaymentDetails details = getPaymentDetails.execute(id);
            Refund refund = refundPayment.execute(new RefundPaymentCommand(
                    details.merchant(), id, Flash.parseAmount(amount), idempotencyKey));
            Flash.success(redirect, "Возврат " + refund.amount().toPlainString() + " ETH отправлен, транзакция "
                    + refund.txHash() + " (статус " + refund.status() + ")");
        } catch (DomainException e) {
            Flash.error(redirect, e);
        } catch (IllegalArgumentException e) {
            Flash.error(redirect, e.getMessage());
        }
        return "redirect:/admin/payments/" + id;
    }
}

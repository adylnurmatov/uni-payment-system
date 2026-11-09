package kg.alatoo.payment.web;

import kg.alatoo.payment.CreatePaymentCommand;
import kg.alatoo.payment.CreatePaymentPort;
import kg.alatoo.payment.GetMerchantPort;
import kg.alatoo.payment.ListClientsPort;
import kg.alatoo.payment.ListMerchantsPort;
import kg.alatoo.payment.exceptions.DomainException;
import kg.alatoo.payment.exceptions.PaymentFailedException;
import kg.alatoo.payment.models.Merchant;
import kg.alatoo.payment.models.Payment;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.UUID;

/**
 * Тестовая оплата из панели: «мерчант / клиент / сумма» → тот же CreatePaymentPort, что и POST /api/v1/payments.
 * Это наглядно показывает, что бизнес-логика не зависит от того, кто её вызывает (REST или веб-форма).
 */
@Controller
@RequestMapping("/admin/test-payment")
public class AdminTestPaymentController {
    private final ListMerchantsPort listMerchants;
    private final ListClientsPort listClients;
    private final GetMerchantPort getMerchant;
    private final CreatePaymentPort createPayment;

    public AdminTestPaymentController(ListMerchantsPort listMerchants,
                                      ListClientsPort listClients,
                                      GetMerchantPort getMerchant,
                                      CreatePaymentPort createPayment) {
        this.listMerchants = listMerchants;
        this.listClients = listClients;
        this.getMerchant = getMerchant;
        this.createPayment = createPayment;
    }

    @GetMapping
    public String form(Model model) {
        model.addAttribute("merchants", listMerchants.execute());
        model.addAttribute("clients", listClients.execute());
        model.addAttribute("idempotencyKey", UUID.randomUUID().toString());
        model.addAttribute("active", "test-payment");
        return "admin/test-payment";
    }

    @PostMapping
    public String pay(@RequestParam UUID merchantId,
                      @RequestParam String clientId,
                      @RequestParam String amount,
                      @RequestParam String idempotencyKey,
                      RedirectAttributes redirect) {
        try {
            Merchant merchant = getMerchant.execute(merchantId);
            Payment payment = createPayment.execute(
                    new CreatePaymentCommand(merchant, clientId, Flash.parseAmount(amount), idempotencyKey));
            Flash.success(redirect, "Платёж создан, статус " + payment.status()
                    + ". Подтверждение придёт автоматически, страница обновляется.");
            return "redirect:/admin/payments/" + payment.id();
        } catch (PaymentFailedException e) {
            // Платёж создан, но FAILED — показываем его страницу с историей статусов
            Flash.error(redirect, e);
            return "redirect:/admin/payments/" + e.getPaymentId();
        } catch (DomainException e) {
            Flash.error(redirect, e);
        } catch (IllegalArgumentException e) {
            Flash.error(redirect, e.getMessage());
        }
        return "redirect:/admin/test-payment";
    }
}

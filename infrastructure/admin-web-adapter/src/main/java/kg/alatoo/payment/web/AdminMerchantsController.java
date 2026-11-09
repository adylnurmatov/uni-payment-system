package kg.alatoo.payment.web;

import kg.alatoo.payment.CreateMerchantCommand;
import kg.alatoo.payment.CreateMerchantPort;
import kg.alatoo.payment.CreateMerchantResult;
import kg.alatoo.payment.ListMerchantsPort;
import kg.alatoo.payment.exceptions.DomainException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Мерчанты: список с балансами и создание. API-ключ показывается один раз — через flash-атрибут,
 * который живёт ровно до следующей страницы и нигде не сохраняется.
 */
@Controller
@RequestMapping("/admin/merchants")
public class AdminMerchantsController {
    private final ListMerchantsPort listMerchants;
    private final CreateMerchantPort createMerchant;

    public AdminMerchantsController(ListMerchantsPort listMerchants, CreateMerchantPort createMerchant) {
        this.listMerchants = listMerchants;
        this.createMerchant = createMerchant;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("merchants", listMerchants.execute());
        model.addAttribute("active", "merchants");
        return "admin/merchants";
    }

    @PostMapping
    public String create(@RequestParam String name, RedirectAttributes redirect) {
        try {
            CreateMerchantResult result = createMerchant.execute(new CreateMerchantCommand(name));
            redirect.addFlashAttribute("createdMerchant", result);
            if (result.fundingError() != null) {
                Flash.error(redirect, "Мерчант создан, но перевод на комиссии не удался: " + result.fundingError());
            }
        } catch (DomainException e) {
            Flash.error(redirect, e);
        }
        return "redirect:/admin/merchants";
    }
}

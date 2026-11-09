package kg.alatoo.payment.web;

import kg.alatoo.payment.CreateClientCommand;
import kg.alatoo.payment.CreateClientPort;
import kg.alatoo.payment.CreateClientResult;
import kg.alatoo.payment.CreateClientWalletPort;
import kg.alatoo.payment.FundClientCommand;
import kg.alatoo.payment.FundClientPort;
import kg.alatoo.payment.FundClientResult;
import kg.alatoo.payment.ListClientsPort;
import kg.alatoo.payment.exceptions.DomainException;
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
 * Клиенты: список с балансами, создание, кошелёк, пополнение из казны.
 * Контроллер вызывает те же use case, что и REST API /api/admin/clients.
 */
@Controller
@RequestMapping("/admin/clients")
public class AdminClientsController {
    private final ListClientsPort listClients;
    private final CreateClientPort createClient;
    private final CreateClientWalletPort createClientWallet;
    private final FundClientPort fundClient;

    public AdminClientsController(ListClientsPort listClients,
                                  CreateClientPort createClient,
                                  CreateClientWalletPort createClientWallet,
                                  FundClientPort fundClient) {
        this.listClients = listClients;
        this.createClient = createClient;
        this.createClientWallet = createClientWallet;
        this.fundClient = fundClient;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("clients", listClients.execute());
        model.addAttribute("active", "clients");
        return "admin/clients";
    }

    @PostMapping
    public String create(@RequestParam String externalId,
                         @RequestParam(defaultValue = "false") boolean createWallet,
                         RedirectAttributes redirect) {
        try {
            CreateClientResult result = createClient.execute(new CreateClientCommand(externalId, createWallet));
            Flash.success(redirect, "Клиент " + result.externalId() + " создан"
                    + (result.walletAddress() == null ? " (без кошелька)" : ", кошелёк " + result.walletAddress()));
        } catch (DomainException e) {
            Flash.error(redirect, e);
        }
        return "redirect:/admin/clients";
    }

    @PostMapping("/{id}/wallet")
    public String createWallet(@PathVariable UUID id, RedirectAttributes redirect) {
        try {
            Flash.success(redirect, "Кошелёк создан: " + createClientWallet.execute(id).walletAddress());
        } catch (DomainException e) {
            Flash.error(redirect, e);
        }
        return "redirect:/admin/clients";
    }

    @PostMapping("/{id}/fund")
    public String fund(@PathVariable UUID id, @RequestParam String amount, RedirectAttributes redirect) {
        try {
            FundClientResult result = fundClient.execute(new FundClientCommand(id, Flash.parseAmount(amount)));
            Flash.success(redirect, "Отправлено " + result.amount().toPlainString() + " ETH из казны, транзакция "
                    + result.txHash() + " (статус " + result.status() + ", подтвердится через ~15–30 с)");
        } catch (DomainException e) {
            Flash.error(redirect, e);
        } catch (IllegalArgumentException e) {
            Flash.error(redirect, e.getMessage());
        }
        return "redirect:/admin/clients";
    }
}

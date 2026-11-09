package kg.alatoo.payment.web;

import kg.alatoo.payment.GetTreasuryPort;
import kg.alatoo.payment.exceptions.DomainException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Главная страница панели — казна, и страница логина.
 */
@Controller
public class AdminHomeController {
    private final GetTreasuryPort getTreasury;

    public AdminHomeController(GetTreasuryPort getTreasury) {
        this.getTreasury = getTreasury;
    }

    @GetMapping("/admin/login")
    public String login() {
        return "admin/login";
    }

    @GetMapping({"/admin", "/admin/"})
    public String treasury(Model model) {
        try {
            model.addAttribute("treasury", getTreasury.execute());
        } catch (DomainException e) {
            model.addAttribute("errorMessage", e.getCode() + ": " + e.getMessage());
        }
        model.addAttribute("active", "treasury");
        return "admin/treasury";
    }
}

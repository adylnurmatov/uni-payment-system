package kg.alatoo.payment.web;

import kg.alatoo.payment.exceptions.DomainException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

/**
 * Ошибки страниц панели (например, платёж не найден) — показываем страницу ошибки, а не стек.
 * Действует только на контроллеры этого пакета; REST-ошибки обрабатывает GlobalExceptionHandler.
 */
@Slf4j
@ControllerAdvice(basePackageClasses = AdminWebExceptionHandler.class)
public class AdminWebExceptionHandler {

    @ExceptionHandler(DomainException.class)
    public String handleDomain(DomainException e, Model model) {
        model.addAttribute("errorMessage", e.getCode() + ": " + e.getMessage());
        return "admin/error";
    }

    @ExceptionHandler(Exception.class)
    public String handleUnexpected(Exception e, Model model) {
        log.error("Admin panel error", e);
        model.addAttribute("errorMessage", "Внутренняя ошибка: " + e.getMessage());
        return "admin/error";
    }
}

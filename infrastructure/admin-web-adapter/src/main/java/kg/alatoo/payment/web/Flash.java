package kg.alatoo.payment.web;

import kg.alatoo.payment.exceptions.DomainException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;

/**
 * Сообщения пользователю после redirect (паттерн Post/Redirect/Get) и разбор сумм из форм.
 */
final class Flash {

    private Flash() {
    }

    static void success(RedirectAttributes attributes, String message) {
        attributes.addFlashAttribute("successMessage", message);
    }

    static void error(RedirectAttributes attributes, String message) {
        attributes.addFlashAttribute("errorMessage", message);
    }

    static void error(RedirectAttributes attributes, DomainException e) {
        error(attributes, e.getCode() + ": " + e.getMessage());
    }

    /** Сумма из поля формы; пустая строка → null, неверный формат → IllegalArgumentException. */
    static BigDecimal parseAmount(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return new BigDecimal(value.trim().replace(',', '.'));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Неверная сумма: " + value);
        }
    }
}

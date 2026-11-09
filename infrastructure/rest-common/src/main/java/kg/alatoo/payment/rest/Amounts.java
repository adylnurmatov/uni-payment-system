package kg.alatoo.payment.rest;

import java.math.BigDecimal;

/**
 * Суммы в JSON отдаём строкой ("0.001"), чтобы клиенты не теряли точность на double.
 */
public final class Amounts {

    private Amounts() {
    }

    public static String format(BigDecimal amount) {
        if (amount == null) {
            return null;
        }
        BigDecimal stripped = amount.stripTrailingZeros();
        return (stripped.scale() < 0 ? stripped.setScale(0) : stripped).toPlainString();
    }
}

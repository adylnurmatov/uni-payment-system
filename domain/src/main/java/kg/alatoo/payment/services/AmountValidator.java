package kg.alatoo.payment.services;

import kg.alatoo.payment.exceptions.DomainException;
import kg.alatoo.payment.exceptions.ErrorCode;
import lombok.experimental.UtilityClass;

import java.math.BigDecimal;

/**
 * Проверка сумм в ETH. Суммы только BigDecimal; у ETH 18 знаков после запятой (1 wei = 1e-18 ETH).
 */
@UtilityClass
public class AmountValidator {
    public static final int ETH_DECIMALS = 18;

    /**
     * @return та же сумма без лишних нулей
     * @throws DomainException INVALID_AMOUNT, если сумма пустая, не положительная или точнее 1 wei
     */
    public static BigDecimal requireValid(BigDecimal amount) {
        if (amount == null) {
            throw new DomainException(ErrorCode.INVALID_AMOUNT, "Amount is required");
        }
        if (amount.signum() <= 0) {
            throw new DomainException(ErrorCode.INVALID_AMOUNT, "Amount must be greater than zero");
        }
        BigDecimal normalized = amount.stripTrailingZeros();
        if (normalized.scale() > ETH_DECIMALS) {
            throw new DomainException(ErrorCode.INVALID_AMOUNT,
                    "Amount has more than " + ETH_DECIMALS + " decimal places (smaller than 1 wei)");
        }
        return normalized.scale() < 0 ? normalized.setScale(0) : normalized;
    }
}

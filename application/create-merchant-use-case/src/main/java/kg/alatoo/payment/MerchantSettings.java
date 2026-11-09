package kg.alatoo.payment;

import java.math.BigDecimal;

/**
 * Настройки создания мерчанта. Бин создаёт boot-модуль из application.yml (merchant.initial-funding).
 *
 * @param initialFunding сколько ETH перевести из казны новому мерчанту на комиссии за возвраты
 */
public record MerchantSettings(BigDecimal initialFunding) {
}

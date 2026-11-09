package kg.alatoo.payment;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * apiKey — открытый API-ключ, возвращается ОДИН раз (в БД хранится только хэш).
 * fundingError != null, если мерчант создан, но перевод на комиссии из казны не удался.
 */
public record CreateMerchantResult(
        UUID merchantId,
        String name,
        String walletAddress,
        String apiKey,
        BigDecimal initialFunding,
        String fundingTxHash,
        String fundingError
) {

    @Override
    public String toString() {
        return "CreateMerchantResult[merchantId=" + merchantId + ", name=" + name + ", apiKey=***]";
    }
}

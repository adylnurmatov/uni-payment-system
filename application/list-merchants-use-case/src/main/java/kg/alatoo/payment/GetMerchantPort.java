package kg.alatoo.payment;

import kg.alatoo.payment.models.Merchant;

import java.util.UUID;

public interface GetMerchantPort {

    /** @throws kg.alatoo.payment.exceptions.DomainException MERCHANT_NOT_FOUND */
    Merchant execute(UUID merchantId);
}

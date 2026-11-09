package kg.alatoo.payment;

import kg.alatoo.payment.models.Merchant;

public interface AuthenticateMerchantPort {

    /**
     * @throws kg.alatoo.payment.exceptions.DomainException UNAUTHORIZED, если ключ пустой или неизвестный
     */
    Merchant execute(String apiKey);
}

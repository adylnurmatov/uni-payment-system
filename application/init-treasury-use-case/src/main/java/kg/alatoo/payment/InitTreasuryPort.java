package kg.alatoo.payment;

import kg.alatoo.payment.models.Wallet;

public interface InitTreasuryPort {

    /** Вернуть казну, создав её, если её ещё нет. */
    Wallet execute();
}

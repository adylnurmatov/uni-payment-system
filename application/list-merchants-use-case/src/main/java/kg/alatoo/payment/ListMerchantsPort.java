package kg.alatoo.payment;

import java.util.List;

public interface ListMerchantsPort {

    /** Все мерчанты с адресом кошелька и балансом (для админ-панели). */
    List<MerchantSummary> execute();
}

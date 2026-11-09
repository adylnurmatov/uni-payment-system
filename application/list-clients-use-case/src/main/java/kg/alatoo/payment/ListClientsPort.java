package kg.alatoo.payment;

import java.util.List;

public interface ListClientsPort {

    /** Все клиенты с адресом кошелька и балансом (для админ-панели). */
    List<ClientSummary> execute();
}

package kg.alatoo.payment;

import java.util.UUID;

public interface CreateClientWalletPort {

    CreateClientWalletResult execute(UUID clientId);
}

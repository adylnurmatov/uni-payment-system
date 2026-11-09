package kg.alatoo.payment;

import kg.alatoo.payment.models.Wallet;

import java.util.Optional;
import java.util.UUID;

public interface WalletRepositoryPort {

    Wallet save(Wallet wallet);

    Optional<Wallet> findById(UUID id);

    Optional<Wallet> findTreasury();
}

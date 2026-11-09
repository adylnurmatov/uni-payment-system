package kg.alatoo.payment.fixtures;

import kg.alatoo.payment.WalletRepositoryPort;
import kg.alatoo.payment.models.Wallet;
import kg.alatoo.payment.models.WalletType;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** In-memory реализация порта для unit-тестов (LSP: взаимозаменяема с JPA-адаптером). */
public class InMemoryWalletRepository implements WalletRepositoryPort {
    private final Map<UUID, Wallet> wallets = new ConcurrentHashMap<>();

    @Override
    public Wallet save(Wallet wallet) {
        wallets.put(wallet.id(), wallet);
        return wallet;
    }

    @Override
    public Optional<Wallet> findById(UUID id) {
        return Optional.ofNullable(wallets.get(id));
    }

    @Override
    public Optional<Wallet> findTreasury() {
        return wallets.values().stream().filter(w -> w.type() == WalletType.TREASURY).findFirst();
    }
}

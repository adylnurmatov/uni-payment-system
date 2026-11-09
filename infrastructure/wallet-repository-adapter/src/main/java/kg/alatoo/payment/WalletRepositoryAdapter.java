package kg.alatoo.payment;

import kg.alatoo.payment.entities.WalletEntity;
import kg.alatoo.payment.models.Wallet;
import kg.alatoo.payment.models.WalletType;
import kg.alatoo.payment.repositories.WalletJpaRepository;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
public class WalletRepositoryAdapter implements WalletRepositoryPort {
    private final WalletJpaRepository repository;

    public WalletRepositoryAdapter(WalletJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public Wallet save(Wallet wallet) {
        return toModel(repository.save(toEntity(wallet)));
    }

    @Override
    public Optional<Wallet> findById(UUID id) {
        return repository.findById(id).map(WalletRepositoryAdapter::toModel);
    }

    @Override
    public Optional<Wallet> findTreasury() {
        return repository.findFirstByType(WalletType.TREASURY.name()).map(WalletRepositoryAdapter::toModel);
    }

    private static Wallet toModel(WalletEntity e) {
        return new Wallet(e.getId(), WalletType.valueOf(e.getType()), e.getAddress(), e.getEncryptedPrivateKey(),
                e.getCreatedAt());
    }

    private static WalletEntity toEntity(Wallet w) {
        return new WalletEntity(w.id(), w.type().name(), w.address(), w.encryptedPrivateKey(), w.createdAt());
    }
}

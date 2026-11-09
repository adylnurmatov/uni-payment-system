package kg.alatoo.payment;

import kg.alatoo.payment.entities.MerchantEntity;
import kg.alatoo.payment.models.Merchant;
import kg.alatoo.payment.repositories.MerchantJpaRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class MerchantRepositoryAdapter implements MerchantRepositoryPort {
    private final MerchantJpaRepository repository;

    public MerchantRepositoryAdapter(MerchantJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public Merchant save(Merchant merchant) {
        return toModel(repository.save(toEntity(merchant)));
    }

    @Override
    public Optional<Merchant> findById(UUID id) {
        return repository.findById(id).map(MerchantRepositoryAdapter::toModel);
    }

    @Override
    public Optional<Merchant> findByApiKeyHash(String apiKeyHash) {
        return repository.findByApiKeyHash(apiKeyHash).map(MerchantRepositoryAdapter::toModel);
    }

    @Override
    public List<Merchant> findAll() {
        return repository.findAll(Sort.by(Sort.Direction.DESC, "createdAt")).stream()
                .map(MerchantRepositoryAdapter::toModel)
                .toList();
    }

    private static Merchant toModel(MerchantEntity e) {
        return new Merchant(e.getId(), e.getName(), e.getApiKeyHash(), e.getWalletId(), e.getCreatedAt());
    }

    private static MerchantEntity toEntity(Merchant m) {
        return new MerchantEntity(m.id(), m.name(), m.apiKeyHash(), m.walletId(), m.createdAt());
    }
}

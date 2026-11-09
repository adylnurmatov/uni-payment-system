package kg.alatoo.payment.fixtures;

import kg.alatoo.payment.MerchantRepositoryPort;
import kg.alatoo.payment.models.Merchant;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryMerchantRepository implements MerchantRepositoryPort {
    private final Map<UUID, Merchant> merchants = new ConcurrentHashMap<>();

    @Override
    public Merchant save(Merchant merchant) {
        merchants.put(merchant.id(), merchant);
        return merchant;
    }

    @Override
    public Optional<Merchant> findById(UUID id) {
        return Optional.ofNullable(merchants.get(id));
    }

    @Override
    public Optional<Merchant> findByApiKeyHash(String apiKeyHash) {
        return merchants.values().stream().filter(m -> m.apiKeyHash().equals(apiKeyHash)).findFirst();
    }

    @Override
    public List<Merchant> findAll() {
        return new ArrayList<>(merchants.values());
    }
}

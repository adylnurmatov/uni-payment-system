package kg.alatoo.payment;

import kg.alatoo.payment.models.Merchant;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MerchantRepositoryPort {

    Merchant save(Merchant merchant);

    Optional<Merchant> findById(UUID id);

    Optional<Merchant> findByApiKeyHash(String apiKeyHash);

    List<Merchant> findAll();
}

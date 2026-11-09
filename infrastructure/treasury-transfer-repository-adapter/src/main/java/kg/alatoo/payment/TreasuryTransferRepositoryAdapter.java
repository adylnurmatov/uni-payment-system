package kg.alatoo.payment;

import kg.alatoo.payment.entities.TreasuryTransferEntity;
import kg.alatoo.payment.models.TreasuryTransfer;
import kg.alatoo.payment.models.TreasuryTransferStatus;
import kg.alatoo.payment.repositories.TreasuryTransferJpaRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class TreasuryTransferRepositoryAdapter implements TreasuryTransferRepositoryPort {
    private final TreasuryTransferJpaRepository repository;

    public TreasuryTransferRepositoryAdapter(TreasuryTransferJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public TreasuryTransfer save(TreasuryTransfer transfer) {
        return toModel(repository.save(toEntity(transfer)));
    }

    @Override
    public Optional<TreasuryTransfer> findById(UUID id) {
        return repository.findById(id).map(TreasuryTransferRepositoryAdapter::toModel);
    }

    @Override
    public List<TreasuryTransfer> findByStatus(TreasuryTransferStatus status) {
        return repository.findByStatus(status.name()).stream().map(TreasuryTransferRepositoryAdapter::toModel).toList();
    }

    private static TreasuryTransfer toModel(TreasuryTransferEntity e) {
        return new TreasuryTransfer(e.getId(), e.getToWalletId(), e.getAmount(), e.getTxHash(),
                TreasuryTransferStatus.valueOf(e.getStatus()), e.getCreatedAt());
    }

    private static TreasuryTransferEntity toEntity(TreasuryTransfer t) {
        return new TreasuryTransferEntity(t.id(), t.toWalletId(), t.amount(), t.txHash(), t.status().name(),
                t.createdAt());
    }
}

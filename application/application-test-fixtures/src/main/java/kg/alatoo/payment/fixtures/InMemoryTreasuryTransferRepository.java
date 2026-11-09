package kg.alatoo.payment.fixtures;

import kg.alatoo.payment.TreasuryTransferRepositoryPort;
import kg.alatoo.payment.models.TreasuryTransfer;
import kg.alatoo.payment.models.TreasuryTransferStatus;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryTreasuryTransferRepository implements TreasuryTransferRepositoryPort {
    private final Map<UUID, TreasuryTransfer> transfers = new ConcurrentHashMap<>();

    @Override
    public TreasuryTransfer save(TreasuryTransfer transfer) {
        transfers.put(transfer.id(), transfer);
        return transfer;
    }

    @Override
    public Optional<TreasuryTransfer> findById(UUID id) {
        return Optional.ofNullable(transfers.get(id));
    }

    @Override
    public List<TreasuryTransfer> findByStatus(TreasuryTransferStatus status) {
        return transfers.values().stream().filter(t -> t.status() == status).toList();
    }
}

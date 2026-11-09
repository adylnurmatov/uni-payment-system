package kg.alatoo.payment;

import kg.alatoo.payment.models.TreasuryTransfer;
import kg.alatoo.payment.models.TreasuryTransferStatus;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TreasuryTransferRepositoryPort {

    TreasuryTransfer save(TreasuryTransfer transfer);

    Optional<TreasuryTransfer> findById(UUID id);

    List<TreasuryTransfer> findByStatus(TreasuryTransferStatus status);
}

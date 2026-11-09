package kg.alatoo.payment.scheduler;

import kg.alatoo.payment.ConfirmTransactionsPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Раз в confirmation.poll-interval-ms (по умолчанию 15 секунд) проверяет PENDING-транзакции.
 * fixedDelay: следующий проход начинается только после окончания предыдущего, поэтому проходы не
 * накладываются, а медленный публичный RPC не получает лишних запросов.
 * Выключается свойством confirmation.scheduler.enabled=false (например, в тестах).
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "confirmation.scheduler.enabled", havingValue = "true", matchIfMissing = true)
public class ConfirmationScheduler {
    private final ConfirmTransactionsPort confirmTransactions;

    public ConfirmationScheduler(ConfirmTransactionsPort confirmTransactions) {
        this.confirmTransactions = confirmTransactions;
    }

    @Scheduled(initialDelayString = "${confirmation.initial-delay-ms:10000}",
            fixedDelayString = "${confirmation.poll-interval-ms:15000}")
    public void run() {
        try {
            confirmTransactions.execute();
        } catch (RuntimeException e) {
            log.error("Confirmation run failed", e);
        }
    }
}

package kg.alatoo.payment;

import java.time.Duration;

/**
 * @param timeout если receipt нет дольше этого времени — транзакция считается неудавшейся (CONFIRMATION_TIMEOUT)
 */
public record ConfirmationSettings(Duration timeout) {
}

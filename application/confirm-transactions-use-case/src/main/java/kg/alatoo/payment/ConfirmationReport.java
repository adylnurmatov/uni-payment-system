package kg.alatoo.payment;

/**
 * Итог одного прохода подтверждений.
 *
 * @param confirmed   сколько транзакций подтвердилось (CONFIRMED / COMPLETED)
 * @param failed      сколько перешло в FAILED (reverted или таймаут)
 * @param stillPending сколько всё ещё ждут блока
 * @param errors      сколько не удалось проверить (ошибка RPC — повторим в следующий раз)
 */
public record ConfirmationReport(int confirmed, int failed, int stillPending, int errors) {

    public boolean isEmpty() {
        return confirmed + failed + stillPending + errors == 0;
    }
}

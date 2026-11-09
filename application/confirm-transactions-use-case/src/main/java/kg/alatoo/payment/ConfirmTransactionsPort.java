package kg.alatoo.payment;

public interface ConfirmTransactionsPort {

    /** Один проход: проверить все PENDING платежи, возвраты и раздачи из казны. */
    ConfirmationReport execute();
}

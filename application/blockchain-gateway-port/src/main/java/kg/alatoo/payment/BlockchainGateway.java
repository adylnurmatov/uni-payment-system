package kg.alatoo.payment;

import kg.alatoo.payment.models.TransactionStatus;

import java.math.BigDecimal;

/**
 * Порт к блокчейну. Бизнес-логика знает только этот интерфейс и ничего не знает про web3j.
 * Реализации: FakeBlockchainGateway (профиль fake) и Web3jBlockchainGateway (профиль sepolia).
 * Все суммы в ETH (BigDecimal), перевод в wei — забота адаптера.
 * Ошибки сети адаптер оборачивает в {@link kg.alatoo.payment.exceptions.BlockchainException}.
 */
public interface BlockchainGateway {

    /** Сгенерировать новую пару ключей (адрес + приватный ключ в hex). */
    GeneratedKeyPair generateKeyPair();

    /** Баланс адреса в ETH. */
    BigDecimal getBalance(String address);

    /**
     * Оценка комиссии сети за перевод ETH (с запасом) — максимум, который может быть списан за газ.
     * Зависит от перевода: например, перевод на ещё не существующий адрес дороже (создаётся аккаунт).
     *
     * @param fromAddress адрес отправителя
     * @param toAddress   адрес получателя; null — «новый, ещё не существующий адрес»
     * @param amount      сумма перевода в ETH
     */
    BigDecimal estimateTransferFee(String fromAddress, String toAddress, BigDecimal amount);

    /**
     * Отправить перевод и НЕ ждать блока.
     *
     * @return хэш транзакции
     */
    String sendTransfer(String fromPrivateKey, String toAddress, BigDecimal amount);

    /** Текущий статус транзакции: PENDING (нет receipt), SUCCESS или FAILED. */
    TransactionStatus getTransactionStatus(String txHash);
}

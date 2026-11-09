package kg.alatoo.payment.exceptions;

/**
 * Ошибка сети или узла блокчейна. Адаптеры (web3j, fake) оборачивают в неё свои исключения,
 * чтобы классы web3j не протекали в бизнес-логику и в ответы API.
 */
public class BlockchainException extends DomainException {

    public BlockchainException(String message) {
        super(ErrorCode.BLOCKCHAIN_ERROR, message);
    }

    public BlockchainException(String message, Throwable cause) {
        super(ErrorCode.BLOCKCHAIN_ERROR, message, cause);
    }
}

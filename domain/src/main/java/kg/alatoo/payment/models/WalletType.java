package kg.alatoo.payment.models;

/**
 * Тип кастодиального кошелька.
 * TREASURY — казна (одна на систему), CLIENT — кошелёк клиента, MERCHANT — кошелёк мерчанта.
 */
public enum WalletType {
    TREASURY,
    CLIENT,
    MERCHANT
}

package kg.alatoo.payment;

public record CreateClientCommand(String externalId, boolean createWallet) {
}

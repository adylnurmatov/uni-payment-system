package kg.alatoo.payment;

/**
 * Ссылки на обозреватель блоков (etherscan). Вынесено в отдельный узкий интерфейс (ISP):
 * контроллерам нужны только ссылки, а не возможность отправлять деньги.
 */
public interface BlockchainExplorer {

    String transactionUrl(String txHash);

    String addressUrl(String address);
}

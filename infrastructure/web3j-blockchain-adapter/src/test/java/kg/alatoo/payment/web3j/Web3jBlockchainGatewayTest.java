package kg.alatoo.payment.web3j;

import kg.alatoo.payment.GeneratedKeyPair;
import org.junit.jupiter.api.Test;
import org.web3j.crypto.Credentials;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Офлайн-тесты: сеть не нужна (Web3j замокан, сетевые методы здесь не вызываются).
 */
class Web3jBlockchainGatewayTest {
    private final Web3jBlockchainGateway gateway = new Web3jBlockchainGateway(
            mock(org.web3j.protocol.Web3j.class), 11155111L, new BigDecimal("1.2"), "https://sepolia.etherscan.io/");

    @Test
    void generatedAddressMatchesPrivateKey() {
        GeneratedKeyPair keyPair = gateway.generateKeyPair();

        assertThat(keyPair.address()).matches("0x[0-9a-f]{40}");
        assertThat(keyPair.privateKeyHex()).matches("[0-9a-f]{64}");
        assertThat(Credentials.create(keyPair.privateKeyHex()).getAddress()).isEqualTo(keyPair.address());
    }

    @Test
    void keyPairsAreUnique() {
        assertThat(gateway.generateKeyPair().address()).isNotEqualTo(gateway.generateKeyPair().address());
    }

    @Test
    void toStringDoesNotLeakPrivateKey() {
        GeneratedKeyPair keyPair = gateway.generateKeyPair();

        assertThat(keyPair.toString()).doesNotContain(keyPair.privateKeyHex());
    }

    @Test
    void buildsEtherscanLinks() {
        assertThat(gateway.transactionUrl("0xabc")).isEqualTo("https://sepolia.etherscan.io/tx/0xabc");
        assertThat(gateway.addressUrl("0xdef")).isEqualTo("https://sepolia.etherscan.io/address/0xdef");
    }
}

package kg.alatoo.payment.web3j;

import kg.alatoo.payment.GeneratedKeyPair;
import kg.alatoo.payment.exceptions.BlockchainException;
import org.junit.jupiter.api.Test;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.Request;
import org.web3j.protocol.core.methods.response.EthGasPrice;
import org.web3j.protocol.exceptions.ClientConnectionException;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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
    @SuppressWarnings("unchecked")
    void rpcRateLimitIsWrappedInBlockchainException() throws Exception {
        Web3j web3j = mock(Web3j.class);
        Request<?, EthGasPrice> request = mock(Request.class);
        when(request.send()).thenThrow(new ClientConnectionException("Invalid response received: 429; Too Many Requests"));
        doReturn(request).when(web3j).ethGasPrice();
        Web3jBlockchainGateway limited = new Web3jBlockchainGateway(web3j, 11155111L, BigDecimal.ONE, "https://x");

        assertThatThrownBy(limited::estimateTransferFee)
                .isInstanceOf(BlockchainException.class)
                .hasMessageContaining("rate limit");
    }

    @Test
    void buildsEtherscanLinks() {
        assertThat(gateway.transactionUrl("0xabc")).isEqualTo("https://sepolia.etherscan.io/tx/0xabc");
        assertThat(gateway.addressUrl("0xdef")).isEqualTo("https://sepolia.etherscan.io/address/0xdef");
    }
}

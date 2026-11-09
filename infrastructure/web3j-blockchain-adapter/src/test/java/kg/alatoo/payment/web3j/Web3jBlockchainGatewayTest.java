package kg.alatoo.payment.web3j;

import kg.alatoo.payment.GeneratedKeyPair;
import kg.alatoo.payment.exceptions.BlockchainException;
import org.junit.jupiter.api.Test;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.Request;
import org.web3j.protocol.core.Response;
import org.web3j.protocol.core.methods.response.EthEstimateGas;
import org.web3j.protocol.core.methods.response.EthGasPrice;
import org.web3j.protocol.exceptions.ClientConnectionException;

import java.math.BigDecimal;
import java.math.BigInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Офлайн-тесты: сеть не нужна, Web3j замокан.
 */
class Web3jBlockchainGatewayTest {
    private static final String FROM = "0x896a85644e9ca015213dd6f28abdac37117c7848";
    private static final String TO = "0x11cf4d69e92807f0c1c380bdaaa20f29bf2dce75";

    private final Web3j web3j = mock(Web3j.class);
    private final Web3jBlockchainGateway gateway = new Web3jBlockchainGateway(web3j, 11155111L,
            new BigDecimal("1.2"), new BigDecimal("1.2"), BigInteger.valueOf(300_000), "https://sepolia.etherscan.io/");

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
    void feeUsesNetworkGasEstimateWithMargin() throws Exception {
        // Реальные значения из Sepolia: перевод на новый адрес 207391 газа, цена газа 638 wei
        mockResponse(new EthEstimateGas(), "0x32a1f", true);
        mockResponse(new EthGasPrice(), "0x27e", false);

        BigDecimal fee = gateway.estimateTransferFee(FROM, TO, new BigDecimal("0.005"));

        BigInteger gasLimit = BigInteger.valueOf(248_870);  // ceil(207391 * 1.2)
        BigInteger gasPrice = BigInteger.valueOf(766);      // ceil(638 * 1.2)
        assertThat(fee).isEqualByComparingTo(new BigDecimal(gasLimit.multiply(gasPrice)).movePointLeft(18));
    }

    @Test
    @SuppressWarnings("unchecked")
    void rpcRateLimitIsWrappedInBlockchainException() throws Exception {
        Request<?, EthEstimateGas> request = mock(Request.class);
        when(request.send()).thenThrow(new ClientConnectionException("Invalid response received: 429; Too Many Requests"));
        doReturn(request).when(web3j).ethEstimateGas(any());

        assertThatThrownBy(() -> gateway.estimateTransferFee(FROM, TO, BigDecimal.ONE))
                .isInstanceOf(BlockchainException.class)
                .hasMessageContaining("rate limit");
    }

    @Test
    void buildsEtherscanLinks() {
        assertThat(gateway.transactionUrl("0xabc")).isEqualTo("https://sepolia.etherscan.io/tx/0xabc");
        assertThat(gateway.addressUrl("0xdef")).isEqualTo("https://sepolia.etherscan.io/address/0xdef");
    }

    @Test
    void rpcUrlIsLoggedWithoutApiKey() {
        assertThat(Web3jConfiguration.hostOf("https://eth-sepolia.g.alchemy.com/v2/secret-key"))
                .isEqualTo("https://eth-sepolia.g.alchemy.com");
    }

    @SuppressWarnings("unchecked")
    private <T extends Response<String>> void mockResponse(T response, String result, boolean estimate) throws Exception {
        response.setResult(result);
        Request<?, T> request = mock(Request.class);
        when(request.send()).thenReturn(response);
        if (estimate) {
            doReturn(request).when(web3j).ethEstimateGas(any());
        } else {
            doReturn(request).when(web3j).ethGasPrice();
        }
    }
}

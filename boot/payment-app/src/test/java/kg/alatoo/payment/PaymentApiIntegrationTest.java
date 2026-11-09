package kg.alatoo.payment;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Сквозной тест REST API на настоящем PostgreSQL (Testcontainers) и fake-блокчейне.
 * Если Docker не запущен — тест пропускается.
 */
@Testcontainers(disabledWithoutDocker = true)
@ActiveProfiles("fake")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "wallet.encryption-key=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=",
        "admin.password=test-admin",
        "abc.base-url=http://localhost:${local.server.port}/mock-abc"
})
class PaymentApiIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private TestRestTemplate rest;

    private String apiKey;
    private String clientId;

    @BeforeEach
    void setUp() {
        JsonNode merchant = admin(HttpMethod.POST, "/api/admin/merchants", Map.of("name", "Shop"), HttpStatus.CREATED);
        apiKey = merchant.get("apiKey").asText();
        assertThat(merchant.get("fundingTxHash").asText()).startsWith("0x");

        String externalId = "ext-" + UUID.randomUUID();
        JsonNode client = admin(HttpMethod.POST, "/api/admin/clients", Map.of("externalId", externalId), HttpStatus.CREATED);
        clientId = externalId;
        admin(HttpMethod.POST, "/api/admin/clients/" + client.get("clientId").asText() + "/fund",
                Map.of("amount", "0.5"), HttpStatus.OK);
    }

    @Test
    void paymentIsCreatedAsPendingAndCanBeRead() {
        ResponseEntity<JsonNode> created = pay(clientId, "0.1", "key-1");

        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(created.getBody().get("status").asText()).isEqualTo("PENDING");
        assertThat(created.getBody().get("txHash").asText()).startsWith("0x");

        String paymentId = created.getBody().get("paymentId").asText();
        ResponseEntity<JsonNode> read = merchantGet("/api/v1/payments/" + paymentId, apiKey);
        assertThat(read.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(read.getBody().get("amount").asText()).isEqualTo("0.1");

        ResponseEntity<JsonNode> list = merchantGet("/api/v1/payments?status=PENDING", apiKey);
        assertThat(list.getBody().get("totalElements").asLong()).isEqualTo(1);
    }

    @Test
    void idempotentReplayAndConflict() {
        String first = pay(clientId, "0.1", "key-1").getBody().get("paymentId").asText();
        ResponseEntity<JsonNode> replay = pay(clientId, "0.1", "key-1");
        ResponseEntity<JsonNode> conflict = pay(clientId, "0.2", "key-1");

        assertThat(replay.getBody().get("paymentId").asText()).isEqualTo(first);
        assertThat(conflict.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(conflict.getBody().get("code").asText()).isEqualTo("IDEMPOTENCY_CONFLICT");
    }

    @Test
    void insufficientFundsReturnsFailedPaymentId() {
        ResponseEntity<JsonNode> response = pay(clientId, "10", "key-1");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(response.getBody().get("code").asText()).isEqualTo("INSUFFICIENT_FUNDS");
        assertThat(response.getBody().get("status").asText()).isEqualTo("FAILED");
        assertThat(response.getBody().get("paymentId").asText()).isNotBlank();
    }

    @Test
    void blockedClientIsRejectedViaAbc() {
        admin(HttpMethod.POST, "/api/admin/clients", Map.of("externalId", "ext-blocked"), HttpStatus.CREATED);

        ResponseEntity<JsonNode> response = pay("ext-blocked", "0.01", "key-1");

        assertThat(response.getBody().get("code").asText()).isEqualTo("CLIENT_BLOCKED");
    }

    @Test
    void invalidApiKeyIsUnauthorized() {
        ResponseEntity<JsonNode> response = merchantGet("/api/v1/payments", "wrong");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody().get("code").asText()).isEqualTo("UNAUTHORIZED");
    }

    @Test
    void adminApiRequiresPassword() {
        ResponseEntity<JsonNode> response = rest.getForEntity("/api/admin/treasury", JsonNode.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    private ResponseEntity<JsonNode> pay(String client, String amount, String idempotencyKey) {
        HttpHeaders headers = jsonHeaders();
        headers.set("X-Api-Key", apiKey);
        headers.set("Idempotency-Key", idempotencyKey);
        return rest.exchange("/api/v1/payments", HttpMethod.POST,
                new HttpEntity<>(Map.of("clientId", client, "amount", amount), headers), JsonNode.class);
    }

    private ResponseEntity<JsonNode> merchantGet(String url, String key) {
        HttpHeaders headers = jsonHeaders();
        headers.set("X-Api-Key", key);
        return rest.exchange(url, HttpMethod.GET, new HttpEntity<>(headers), JsonNode.class);
    }

    private JsonNode admin(HttpMethod method, String url, Object body, HttpStatus expected) {
        ResponseEntity<JsonNode> response = rest.withBasicAuth("admin", "test-admin")
                .exchange(url, method, new HttpEntity<>(body, jsonHeaders()), JsonNode.class);
        assertThat(response.getStatusCode()).as(url + " -> " + response.getBody()).isEqualTo(expected);
        return response.getBody();
    }

    private static HttpHeaders jsonHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }
}

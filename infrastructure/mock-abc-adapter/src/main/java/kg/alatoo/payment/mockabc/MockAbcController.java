package kg.alatoo.payment.mockabc;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Имитация внешнего «сервиса ABC». Данные — из seed-файла mock-abc/clients.json.
 * Правила для демо:
 * <ul>
 *     <li>клиент есть в seed-файле → его данные;</li>
 *     <li>externalId начинается с "unknown-" → 404 (ABC не знает клиента);</li>
 *     <li>externalId начинается с "unavailable-" → 503 (ABC «лежит»);</li>
 *     <li>иначе → сгенерированный ACTIVE-профиль, чтобы любого созданного клиента можно было оплатить.</li>
 * </ul>
 */
@RestController
@RequestMapping("/mock-abc")
@Tag(name = "Mock ABC", description = "Имитация внешнего сервиса данных клиентов")
public class MockAbcController {
    private final Map<String, AbcClient> seed;

    public MockAbcController(ObjectMapper objectMapper) throws IOException {
        try (InputStream in = new ClassPathResource("mock-abc/clients.json").getInputStream()) {
            List<AbcClient> clients = objectMapper.readValue(in, new TypeReference<>() {
            });
            this.seed = clients.stream().collect(Collectors.toMap(AbcClient::externalId, Function.identity()));
        }
    }

    @GetMapping("/clients/{externalId}")
    @Operation(summary = "Данные клиента по externalId")
    public ResponseEntity<AbcClient> getClient(@PathVariable String externalId) {
        if (externalId.startsWith("unknown-")) {
            return ResponseEntity.notFound().build();
        }
        if (externalId.startsWith("unavailable-")) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
        }
        AbcClient client = seed.getOrDefault(externalId,
                new AbcClient(externalId, "Client " + externalId, externalId + "@example.com", "KG", "ACTIVE"));
        return ResponseEntity.ok(client);
    }

    public record AbcClient(String externalId, String fullName, String email, String country, String status) {
    }
}

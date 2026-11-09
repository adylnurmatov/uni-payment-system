package kg.alatoo.payment.abc;

import kg.alatoo.payment.ClientDataProvider;
import kg.alatoo.payment.exceptions.DomainException;
import kg.alatoo.payment.exceptions.ErrorCode;
import kg.alatoo.payment.models.ClientProfile;
import kg.alatoo.payment.models.ClientProfileStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Duration;

/**
 * Ходит во внешний «сервис ABC» по HTTP (честный внешний вызов через RestClient).
 * Сейчас ABC — это mock внутри приложения, но base URL берётся из конфига (ABC_BASE_URL),
 * поэтому сервис можно вынести отдельно, не меняя код.
 */
@Slf4j
@Component
public class HttpClientDataProvider implements ClientDataProvider {
    private final RestClient restClient;
    private final Environment environment;

    public HttpClientDataProvider(Environment environment) {
        this.environment = environment;
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(environment.getProperty("abc.connect-timeout", Duration.class, Duration.ofSeconds(2)));
        requestFactory.setReadTimeout(environment.getProperty("abc.read-timeout", Duration.class, Duration.ofSeconds(5)));
        this.restClient = RestClient.builder().requestFactory(requestFactory).build();
    }

    @Override
    public ClientProfile getClientProfile(String externalId) {
        String url = baseUrl() + "/clients/{externalId}";
        try {
            AbcClientResponse response = restClient.get()
                    .uri(url, externalId)
                    .retrieve()
                    .body(AbcClientResponse.class);
            if (response == null || response.status() == null) {
                throw new DomainException(ErrorCode.CLIENT_DATA_UNAVAILABLE, "ABC returned an empty response");
            }
            return new ClientProfile(response.externalId(), response.fullName(), response.email(),
                    response.country(), parseStatus(response.status()));
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode().isSameCodeAs(HttpStatus.NOT_FOUND)) {
                throw new DomainException(ErrorCode.CLIENT_NOT_FOUND, "Client '" + externalId + "' is unknown to ABC");
            }
            log.warn("ABC rejected request for client {}: {}", externalId, e.getStatusCode());
            throw new DomainException(ErrorCode.CLIENT_DATA_UNAVAILABLE, "ABC returned " + e.getStatusCode().value());
        } catch (RestClientException e) {
            log.warn("ABC is unavailable for client {}: {}", externalId, e.getMessage());
            throw new DomainException(ErrorCode.CLIENT_DATA_UNAVAILABLE, "Client data service ABC is unavailable");
        }
    }

    /** Читается при каждом вызове: в тестах порт сервера становится известен только после старта. */
    private String baseUrl() {
        String url = environment.getProperty("abc.base-url");
        if (url == null || url.isBlank()) {
            throw new DomainException(ErrorCode.CLIENT_DATA_UNAVAILABLE, "ABC_BASE_URL is not configured");
        }
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }

    private static ClientProfileStatus parseStatus(String status) {
        try {
            return ClientProfileStatus.valueOf(status);
        } catch (IllegalArgumentException e) {
            throw new DomainException(ErrorCode.CLIENT_DATA_UNAVAILABLE, "ABC returned unknown client status " + status);
        }
    }

    /** Формат ответа ABC (контракт внешнего сервиса, отдельно от доменной модели). */
    record AbcClientResponse(String externalId, String fullName, String email, String country, String status) {
    }
}

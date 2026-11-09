package kg.alatoo.payment.fixtures;

import kg.alatoo.payment.ClientDataProvider;
import kg.alatoo.payment.exceptions.DomainException;
import kg.alatoo.payment.exceptions.ErrorCode;
import kg.alatoo.payment.models.ClientProfile;
import kg.alatoo.payment.models.ClientProfileStatus;

import java.util.HashSet;
import java.util.Set;

/**
 * Заглушка «сервиса ABC»: все клиенты ACTIVE, кроме заблокированных; можно «уронить» сервис.
 */
public class StubClientDataProvider implements ClientDataProvider {
    private final Set<String> blocked = new HashSet<>();
    private boolean available = true;

    public void block(String externalId) {
        blocked.add(externalId);
    }

    public void setAvailable(boolean available) {
        this.available = available;
    }

    @Override
    public ClientProfile getClientProfile(String externalId) {
        if (!available) {
            throw new DomainException(ErrorCode.CLIENT_DATA_UNAVAILABLE, "ABC is unavailable");
        }
        ClientProfileStatus status = blocked.contains(externalId) ? ClientProfileStatus.BLOCKED : ClientProfileStatus.ACTIVE;
        return new ClientProfile(externalId, "Test " + externalId, externalId + "@example.com", "KG", status);
    }
}

package kg.alatoo.payment.models;

/**
 * Данные клиента из внешнего «сервиса ABC».
 */
public record ClientProfile(
        String externalId,
        String fullName,
        String email,
        String country,
        ClientProfileStatus status
) {

    public boolean isBlocked() {
        return status == ClientProfileStatus.BLOCKED;
    }
}

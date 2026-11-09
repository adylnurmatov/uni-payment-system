package kg.alatoo.payment;

import kg.alatoo.payment.models.ClientProfile;

/**
 * Порт к внешнему «сервису ABC», который знает данные клиента.
 * Реализация: HttpClientDataProvider (ходит по HTTP).
 *
 * @throws kg.alatoo.payment.exceptions.DomainException CLIENT_DATA_UNAVAILABLE если сервис недоступен,
 *                                                      CLIENT_NOT_FOUND если ABC не знает клиента
 */
public interface ClientDataProvider {

    ClientProfile getClientProfile(String externalId);
}

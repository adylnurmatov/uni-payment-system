package kg.alatoo.payment;

import java.util.UUID;

public interface GetClientBalancePort {

    GetClientBalanceResult execute(UUID clientId);
}

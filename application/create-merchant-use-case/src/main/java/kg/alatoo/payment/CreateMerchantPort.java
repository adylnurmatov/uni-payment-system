package kg.alatoo.payment;

public interface CreateMerchantPort {

    CreateMerchantResult execute(CreateMerchantCommand command);
}

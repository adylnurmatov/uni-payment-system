package kg.alatoo.payment;

public interface FundClientPort {

    FundClientResult execute(FundClientCommand command);
}

package kg.alatoo.payment.admin;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import kg.alatoo.payment.BlockchainExplorer;
import kg.alatoo.payment.CreateClientCommand;
import kg.alatoo.payment.CreateClientPort;
import kg.alatoo.payment.CreateClientResult;
import kg.alatoo.payment.CreateClientWalletPort;
import kg.alatoo.payment.CreateClientWalletResult;
import kg.alatoo.payment.FundClientCommand;
import kg.alatoo.payment.FundClientPort;
import kg.alatoo.payment.FundClientResult;
import kg.alatoo.payment.GetClientBalancePort;
import kg.alatoo.payment.GetClientBalanceResult;
import kg.alatoo.payment.rest.Amounts;
import kg.alatoo.payment.rest.OpenApiConfiguration;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/clients")
@Tag(name = "Admin: clients")
@SecurityRequirement(name = OpenApiConfiguration.ADMIN_BASIC)
public class AdminClientController {
    private final CreateClientPort createClient;
    private final CreateClientWalletPort createClientWallet;
    private final FundClientPort fundClient;
    private final GetClientBalancePort getClientBalance;
    private final BlockchainExplorer explorer;

    public AdminClientController(CreateClientPort createClient,
                                 CreateClientWalletPort createClientWallet,
                                 FundClientPort fundClient,
                                 GetClientBalancePort getClientBalance,
                                 BlockchainExplorer explorer) {
        this.createClient = createClient;
        this.createClientWallet = createClientWallet;
        this.fundClient = fundClient;
        this.getClientBalance = getClientBalance;
        this.explorer = explorer;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Создать клиента (по желанию сразу с кошельком)")
    public ClientResponse create(@RequestBody @Valid CreateClientRequest request) {
        boolean createWallet = request.createWallet() == null || request.createWallet();
        CreateClientResult result = createClient.execute(new CreateClientCommand(request.externalId(), createWallet));
        return new ClientResponse(result.clientId(), result.externalId(), result.walletAddress());
    }

    @PostMapping("/{id}/wallet")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Создать кошелёк существующему клиенту")
    public ClientResponse createWallet(@PathVariable UUID id) {
        CreateClientWalletResult result = createClientWallet.execute(id);
        return new ClientResponse(result.clientId(), null, result.walletAddress());
    }

    @PostMapping("/{id}/fund")
    @Operation(summary = "Перевести клиенту ETH из казны")
    public FundResponse fund(@PathVariable UUID id, @RequestBody @Valid FundRequest request) {
        FundClientResult result = fundClient.execute(new FundClientCommand(id, request.amount()));
        return new FundResponse(result.transferId(), result.toAddress(), Amounts.format(result.amount()),
                result.txHash(), explorer.transactionUrl(result.txHash()), result.status().name());
    }

    @GetMapping("/{id}/balance")
    @Operation(summary = "Баланс кошелька клиента")
    public BalanceResponse balance(@PathVariable UUID id) {
        GetClientBalanceResult result = getClientBalance.execute(id);
        return new BalanceResponse(result.clientId(), result.externalId(), result.address(),
                Amounts.format(result.balance()), "ETH", explorer.addressUrl(result.address()));
    }

    public record CreateClientRequest(
            @NotBlank @Size(max = 128) @Schema(example = "ext-42") String externalId,
            @Schema(description = "Создать кошелёк сразу (по умолчанию true)", example = "true") Boolean createWallet) {
    }

    public record FundRequest(
            @NotNull @Schema(type = "string", example = "0.05") BigDecimal amount) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record ClientResponse(UUID clientId, String externalId, String walletAddress) {
    }

    public record FundResponse(UUID transferId, String toAddress, String amount, String txHash,
                               String explorerUrl, String status) {
    }

    public record BalanceResponse(UUID clientId, String externalId, String address, String balance,
                                  String currency, String explorerUrl) {
    }
}

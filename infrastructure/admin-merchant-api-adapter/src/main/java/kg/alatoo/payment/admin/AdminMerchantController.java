package kg.alatoo.payment.admin;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import kg.alatoo.payment.BlockchainExplorer;
import kg.alatoo.payment.CreateMerchantCommand;
import kg.alatoo.payment.CreateMerchantPort;
import kg.alatoo.payment.CreateMerchantResult;
import kg.alatoo.payment.rest.Amounts;
import kg.alatoo.payment.rest.OpenApiConfiguration;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/admin/merchants")
@Tag(name = "Admin: merchants")
@SecurityRequirement(name = OpenApiConfiguration.ADMIN_BASIC)
public class AdminMerchantController {
    private final CreateMerchantPort createMerchant;
    private final BlockchainExplorer explorer;

    public AdminMerchantController(CreateMerchantPort createMerchant, BlockchainExplorer explorer) {
        this.createMerchant = createMerchant;
        this.explorer = explorer;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Создать мерчанта",
            description = "Создаёт кошелёк, переводит ETH из казны на комиссии и возвращает API-ключ. "
                    + "Ключ показывается ОДИН раз — сохраните его.")
    public MerchantCreatedResponse create(@RequestBody @Valid CreateMerchantRequest request) {
        CreateMerchantResult result = createMerchant.execute(new CreateMerchantCommand(request.name()));
        return new MerchantCreatedResponse(result.merchantId(), result.name(), result.walletAddress(),
                result.apiKey(), Amounts.format(result.initialFunding()), result.fundingTxHash(),
                result.fundingTxHash() == null ? null : explorer.transactionUrl(result.fundingTxHash()),
                result.fundingError());
    }

    public record CreateMerchantRequest(@NotBlank @Size(max = 255) @Schema(example = "Coffee Shop") String name) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record MerchantCreatedResponse(UUID merchantId, String name, String walletAddress, String apiKey,
                                          String initialFunding, String fundingTxHash, String fundingExplorerUrl,
                                          String fundingError) {
    }
}

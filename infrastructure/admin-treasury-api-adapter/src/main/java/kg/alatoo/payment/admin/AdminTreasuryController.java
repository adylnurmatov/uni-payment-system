package kg.alatoo.payment.admin;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import kg.alatoo.payment.BlockchainExplorer;
import kg.alatoo.payment.GetTreasuryPort;
import kg.alatoo.payment.GetTreasuryResult;
import kg.alatoo.payment.rest.Amounts;
import kg.alatoo.payment.rest.OpenApiConfiguration;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/treasury")
@Tag(name = "Admin: treasury")
@SecurityRequirement(name = OpenApiConfiguration.ADMIN_BASIC)
public class AdminTreasuryController {
    private final GetTreasuryPort getTreasury;
    private final BlockchainExplorer explorer;

    public AdminTreasuryController(GetTreasuryPort getTreasury, BlockchainExplorer explorer) {
        this.getTreasury = getTreasury;
        this.explorer = explorer;
    }

    @GetMapping
    @Operation(summary = "Адрес и баланс казны")
    public TreasuryResponse get() {
        GetTreasuryResult result = getTreasury.execute();
        return new TreasuryResponse(result.address(), Amounts.format(result.balance()), "ETH",
                explorer.addressUrl(result.address()));
    }

    public record TreasuryResponse(String address, String balance, String currency, String explorerUrl) {
    }
}

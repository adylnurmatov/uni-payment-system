package kg.alatoo.payment.web;

import kg.alatoo.payment.BlockchainExplorer;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * Хелпер для Thymeleaf-шаблонов, доступен как ${@ui...}: цвета статусов, ссылки на etherscan, форматирование.
 */
@Component("ui")
public class AdminUi {
    private static final DateTimeFormatter DATE_TIME =
            DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss").withZone(ZoneId.systemDefault());

    private final BlockchainExplorer explorer;

    public AdminUi(BlockchainExplorer explorer) {
        this.explorer = explorer;
    }

    /** CSS-класс бейджа Bootstrap для статуса платежа, возврата или раздачи. */
    public String badge(Object status) {
        if (status == null) {
            return "text-bg-light";
        }
        return switch (status.toString()) {
            case "PENDING" -> "text-bg-warning";
            case "CONFIRMED", "COMPLETED" -> "text-bg-success";
            case "PARTIALLY_REFUNDED" -> "text-bg-info";
            case "REFUNDED" -> "text-bg-primary";
            case "FAILED" -> "text-bg-danger";
            default -> "text-bg-secondary";
        };
    }

    public String amount(BigDecimal amount) {
        if (amount == null) {
            return "—";
        }
        BigDecimal stripped = amount.stripTrailingZeros();
        return (stripped.scale() < 0 ? stripped.setScale(0) : stripped).toPlainString();
    }

    /** 0x1234…abcd — короткая форма адреса или хэша для таблиц. */
    public String shortHex(String value) {
        if (value == null) {
            return "—";
        }
        return value.length() <= 14 ? value : value.substring(0, 8) + "…" + value.substring(value.length() - 6);
    }

    public String txUrl(String txHash) {
        return txHash == null ? null : explorer.transactionUrl(txHash);
    }

    public String addressUrl(String address) {
        return address == null ? null : explorer.addressUrl(address);
    }

    /** Ссылки fake-режима (fake-explorer://) браузер открыть не может — их не показываем как ссылки. */
    public boolean isWebLink(String url) {
        return url != null && url.startsWith("http");
    }

    public String time(Instant instant) {
        return instant == null ? "—" : DATE_TIME.format(instant);
    }
}

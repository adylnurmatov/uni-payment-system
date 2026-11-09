package kg.alatoo.payment;

import lombok.experimental.UtilityClass;

import java.security.SecureRandom;

/**
 * Генерирует случайный API-ключ мерчанта вида "pk_" + 40 символов.
 */
@UtilityClass
public class ApiKeyGenerator {
    private static final String ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    private static final int LENGTH = 40;
    private static final SecureRandom RANDOM = new SecureRandom();

    public static String generate() {
        StringBuilder key = new StringBuilder("pk_");
        for (int i = 0; i < LENGTH; i++) {
            key.append(ALPHABET.charAt(RANDOM.nextInt(ALPHABET.length())));
        }
        return key.toString();
    }
}

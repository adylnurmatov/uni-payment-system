package kg.alatoo.payment.models;

/**
 * Параметры страницы (page с нуля).
 */
public record PageQuery(int page, int size) {
    public static final int MAX_SIZE = 100;

    public PageQuery {
        if (page < 0) {
            page = 0;
        }
        if (size <= 0) {
            size = 20;
        }
        if (size > MAX_SIZE) {
            size = MAX_SIZE;
        }
    }
}

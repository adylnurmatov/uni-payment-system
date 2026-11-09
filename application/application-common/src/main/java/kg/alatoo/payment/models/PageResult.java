package kg.alatoo.payment.models;

import java.util.List;
import java.util.function.Function;

/**
 * Страница результатов без зависимости от Spring Data.
 */
public record PageResult<T>(List<T> items, int page, int size, long totalElements) {

    public int totalPages() {
        return size == 0 ? 0 : (int) Math.ceil((double) totalElements / size);
    }

    public <R> PageResult<R> map(Function<T, R> mapper) {
        return new PageResult<>(items.stream().map(mapper).toList(), page, size, totalElements);
    }
}

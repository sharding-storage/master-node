package team.brown.sharding.master.grpc;

import lombok.Builder;
import lombok.experimental.Accessors;

import java.util.Objects;

/**
 * Класс для представления диапазона хеш-значений.
 * Используется при миграции данных между шардами.
 */
@Accessors(chain = true)
public class HashRange implements Comparable<HashRange> {
    private final int start;  // Начало диапазона (включительно)
    private final int end;    // Конец диапазона (включительно)

    /**
     * Создает новый диапазон хешей.
     *
     * @param start начало диапазона (включительно)
     * @param end конец диапазона (включительно)
     * @throws IllegalArgumentException если start > end
     */
    public HashRange(int start, int end) {
        this.start = start;
        this.end = end;
    }

    public int getStart() {
        return start;
    }

    public int getEnd() {
        return end;
    }

    /**
     * Проверяет, попадает ли хеш в этот диапазон.
     *
     * @param hashValue значение хеша для проверки
     * @return true если значение в диапазоне [start, end]
     */
    public boolean contains(long hashValue) {
        return hashValue >= start && hashValue <= end;
    }

    /**
     * Проверяет, пересекается ли этот диапазон с другим.
     *
     * @param other другой диапазон
     * @return true если диапазоны пересекаются
     */
    public boolean overlaps(HashRange other) {
        return this.start <= other.end && this.end >= other.start;
    }

    /**
     * Сравнивает диапазоны по начальному значению.
     * Используется для сортировки.
     */
    @Override
    public int compareTo(HashRange other) {
        return Long.compare(this.start, other.start);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        HashRange hashRange = (HashRange) o;
        return start == hashRange.start && end == hashRange.end;
    }

    @Override
    public int hashCode() {
        return Objects.hash(start, end);
    }

    @Override
    public String toString() {
        return "[" + start + ", " + end + "]";
    }

    /**
     * Создает диапазон, представляющий объединение двух диапазонов.
     *
     * @param a первый диапазон
     * @param b второй диапазон
     * @return новый объединенный диапазон
     * @throws IllegalArgumentException если диапазоны не пересекаются
     */
    public static HashRange merge(HashRange a, HashRange b) {
        if (!a.overlaps(b)) {
            throw new IllegalArgumentException("Ranges do not overlap");
        }
        return new HashRange(Math.min(a.start, b.start), Math.max(a.end, b.end));
    }
}
package team.brown.sharding.master.hash;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collection;
import java.util.SortedMap;
import java.util.TreeMap;

import java.util.Collection;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * Кольцо консистентного хеширования с поддержкой виртуальных узлов.
 */
public class ConsistentHashRing<T> {

    private final SortedMap<Integer, T> circle = new TreeMap<>();
    private final HashFunction hashFunction;
    private final int virtualNodes;

    /**
     * Конструктор.
     * @param hashFunction хеш-функция
     * @param nodes исходные узлы
     * @param virtualNodes количество виртуальных узлов на каждый сервер
     */
    public ConsistentHashRing(HashFunction hashFunction, Collection<T> nodes, int virtualNodes) {
        this.hashFunction = hashFunction;
        this.virtualNodes = virtualNodes;
        for (T node : nodes) {
            addNode(node);
        }
    }

    /**
     * Добавляет узел в кольцо (с виртуальными узлами).
     * @param node узел
     */
    public void addNode(T node) {
        for (int i = 0; i < virtualNodes; i++) {
            int hash = hashFunction.hash(node.toString() + "-" + i);
            circle.put(hash, node);
        }
    }

    /**
     * Удаляет узел из кольца.
     * @param node узел
     */
    public void removeNode(T node) {
        for (int i = 0; i < virtualNodes; i++) {
            int hash = hashFunction.hash(node.toString() + "-" + i);
            circle.remove(hash);
        }
    }

    /**
     * Возвращает узел, ответственный за заданный ключ.
     * @param key ключ
     * @return узел или null, если кольцо пустое
     */
    public T getNode(Object key) {
        if (circle.isEmpty()) {
            return null;
        }
        int hash = hashFunction.hash(key.toString());
        if (!circle.containsKey(hash)) {
            SortedMap<Integer, T> tailMap = circle.tailMap(hash);
            hash = tailMap.isEmpty() ? circle.firstKey() : tailMap.firstKey();
        }
        return circle.get(hash);
    }

    /**
     * Очищает кольцо.
     */
    public void clear() {
        circle.clear();
    }

    /**
     * Интерфейс хеш-функции.
     */
    public interface HashFunction {
        int hash(String key);
    }

    /**
     * Реализация хеш-функции MD5.
     */
    public static class MD5HashFunction implements HashFunction {
        @Override
        public int hash(String key) {
            try {
                java.security.MessageDigest md = java.security.MessageDigest.getInstance("MD5");
                byte[] digest = md.digest(key.getBytes());
                return new java.math.BigInteger(1, digest).intValue();
            } catch (java.security.NoSuchAlgorithmException e) {
                throw new RuntimeException("MD5 algorithm not found", e);
            }
        }
    }
}
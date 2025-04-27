package team.brown.sharding.master.hash;

import lombok.extern.slf4j.Slf4j;
import team.brown.sharding.master.grpc.HashRange;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * Кольцо консистентного хеширования с поддержкой виртуальных узлов.
 */
@Slf4j
public class ConsistentHashRing<T> implements Cloneable{

    private SortedMap<Integer, T> circle = new TreeMap<>();
    private final HashFunction hashFunction;
    private final int virtualNodes;

    /**
     * Конструктор.
     *
     * @param hashFunction хеш-функция
     * @param nodes        исходные узлы
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
     *
     * @param node узел
     */
    public SortedMap<Integer, T> addNode(T node) {
        log.info("Add node to ring: node={}", node);
        for (int i = 0; i < virtualNodes; i++) {
            int hash = hashFunction.hash(node.toString() + "-" + i);
            circle.put(hash, node);
        }
        return circle;
    }

    /**
     * Удаляет узел из кольца.
     *
     * @param node узел
     */
    public SortedMap<Integer, T> removeNode(T node) {
        log.info("Remove node from ring: node={}", node);
        for (int i = 0; i < virtualNodes; i++) {
            int hash = hashFunction.hash(node.toString() + "-" + i);
            circle.remove(hash);
        }
        return circle;
    }

    /**
     * Возвращает узел, ответственный за заданный ключ.
     *
     * @param key ключ
     * @return узел или null, если кольцо пустое
     */
    public T getNode(Object key) {
        log.info("Get node for key: key={}", key);
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

    public T getNodeByHash(int hash) {
        log.info("Get node by hash: hash={}", hash);
        if (circle.isEmpty()) {
            return null;
        }
        SortedMap<Integer, T> tailMap = circle.tailMap(hash);
        if (!tailMap.isEmpty()) {
            return tailMap.get(tailMap.firstKey());
        } else {
            // Когда tailMap пустой, берем самый первый узел по кругу
            return circle.get(circle.firstKey());
        }
    }

    /**
     * Очищает кольцо.
     */
    public void clear() {
        log.info("Clear ring");
        circle.clear();
    }

    public int getVirtualNodes() {
        log.info("Get virtual nodes count");
        return virtualNodes;
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

    @Override
    public ConsistentHashRing<T> clone() {
        try {
            // Поверхностное копирование примитивных полей и final-ссылок
            ConsistentHashRing<T> cloned = (ConsistentHashRing<T>) super.clone();

            // Глубокое копирование изменяемого состояния (SortedMap)
            cloned.circle = new TreeMap<>(this.circle); // Копируем все записи

            // hashFunction и virtualNodes не нужно копировать, т.к. они final и неизменяемы
            return cloned;
        } catch (CloneNotSupportedException e) {
            throw new AssertionError("Clone not supported", e); // Не должно произойти
        }
    }

    public Set<T> getNodes() {
        log.info("Get all nodes from ring");
        return new HashSet<>(circle.values());
    }

    public List<HashRange> getHashRanges(T node) {
        log.info("Get hash ranges for node: node={}", node);
        List<HashRange> ranges = new ArrayList<>();
        if (circle.isEmpty()) {
            return ranges;
        }

        // Собираем все хэши для данного узла
        List<Integer> nodeHashes = new ArrayList<>();
        for (Map.Entry<Integer, T> entry : circle.entrySet()) {
            if (entry.getValue().equals(node)) {
                nodeHashes.add(entry.getKey());
            }
        }

        if (nodeHashes.isEmpty()) {
            return ranges;
        }

        Collections.sort(nodeHashes);

        for (Integer hash : nodeHashes) {
            SortedMap<Integer, T> headMap = circle.headMap(hash);
            int startHash;
            if (headMap.isEmpty()) {
                if (!circle.get(circle.lastKey()).equals(node)) {
                    startHash = circle.lastKey() + 1;
                } else {
                    startHash = circle.lastKey();
                }
            } else {
                if (!headMap.get(headMap.lastKey()).equals(node)) {
                    startHash = headMap.lastKey() + 1;
                } else {
                    startHash = headMap.lastKey();
                }
            }
            ranges.add(new HashRange(startHash, hash));
        }

        return ranges;
    }

    /**
     * Возвращает узел для указанного хеша.
     *
     * @param hash значение хеша
     * @return узел, ответственный за этот хеш
     */
    public T getNodeForHash(int hash) {
        log.info("Get node for hash: hash={}", hash);
        if (circle.isEmpty()) {
            return null;
        }

        if (!circle.containsKey(hash)) {
            SortedMap<Integer, T> tailMap = circle.tailMap(hash);
            hash = tailMap.isEmpty() ? circle.firstKey() : tailMap.firstKey();
        }

        return circle.get(hash);
    }

    public SortedMap<Integer, T> getCircle(){
        log.info("Get circle state");
        return this.circle;
    }
}
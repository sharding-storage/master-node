package team.brown.sharding.master.node;

import org.springframework.stereotype.Component;
import team.brown.sharding.master.hash.ConsistentHashRing;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * Пример мастер-узла, управляющего схемой шардирования.
 */
@Component
public class MasterNode {
    /**
     * Количество виртуальных узлов на сервер
     */
    private static final int DEFAULT_NODE_PER_SERVER = 1;

    // Множество зарегистрированных серверов.
    private final Set<ServerNode> nodes;
    // Кольцо консистентного хеширования (не final, чтобы его можно было пересоздавать при решардинге).
    private ConsistentHashRing<ServerNode> ring;

    /**
     * Инициализация MasterNode с изначальным набором серверов и числом виртуальных узлов.
     *
     * @param initialNodes исходные узлы
     */
    public MasterNode(Collection<ServerNode> initialNodes) {
        this.nodes = new HashSet<>(initialNodes);
        this.ring = new ConsistentHashRing<>(
                new ConsistentHashRing.MD5HashFunction(),
                nodes,
                DEFAULT_NODE_PER_SERVER
        );
    }

    /**
     * Добавляет сервер в кольцо.
     *
     * @param node сервер (ip:port)
     * @return true, если сервер добавлен впервые; false иначе
     */
    public synchronized boolean addServer(ServerNode node) {
        if (nodes.contains(node)) {
            return false;
        }
        nodes.add(node);
        ring.addNode(node);
        return true;
    }

    /**
     * Удаляет сервер из кольца.
     *
     * @param node сервер
     * @return true, если сервер удалён; false если его не было
     */
    public synchronized boolean removeServer(ServerNode node) {
        if (!nodes.contains(node)) {
            return false;
        }
        nodes.remove(node);
        ring.removeNode(node);
        return true;
    }

    /**
     * Возвращает сервер, ответственный за данный ключ.
     *
     * @param key ключ
     * @return сервер
     */
    public synchronized ServerNode getServerForKey(String key) {
        return ring.getNode(key);
    }

    /**
     * Возвращает текущее множество серверов.
     *
     * @return множество серверов
     */
    public synchronized Set<ServerNode> getNodes() {
        return new HashSet<>(nodes);
    }

    /**
     * Обновляет число виртуальных узлов (решардинг).
     *
     * @param newVirtualNodes новое количество виртуальных узлов
     */
    public synchronized void updateShardCount(int newVirtualNodes) {
        // Пересоздаем кольцо с новым числом виртуальных узлов
        this.ring = new ConsistentHashRing<>(
                new ConsistentHashRing.MD5HashFunction(),
                nodes,
                newVirtualNodes
        );
    }
}
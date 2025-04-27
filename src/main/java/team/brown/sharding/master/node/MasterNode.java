package team.brown.sharding.master.node;

import org.springframework.stereotype.Component;
import team.brown.sharding.master.grpc.HashRange;
import team.brown.sharding.master.grpc.RestClient;
import team.brown.sharding.master.hash.ConsistentHashRing;

import java.util.*;

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
    private final RestClient restClient;

    /**
     * Инициализация MasterNode с изначальным набором серверов и числом виртуальных узлов.
     *
     * @param initialNodes исходные узлы
     */
    public MasterNode(Collection<ServerNode> initialNodes, RestClient restClient) {
        this.nodes = new HashSet<>(initialNodes);
        this.ring = new ConsistentHashRing<>(
                new ConsistentHashRing.MD5HashFunction(),
                nodes,
                DEFAULT_NODE_PER_SERVER
        );
        this.restClient = restClient;
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
        if (nodes.size() == 1) {
            ring.addNode(node);
            return true;
        }
        ConsistentHashRing<ServerNode> oldRing = this.ring.clone();
        ring.addNode(node);
        Map<ServerNode, List<HashRange>> migrationPlan = calculateMigrationRanges(oldRing, ring);
        executeRestRangeMigration(migrationPlan, oldRing);
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
     * Возвращает текущее множество серверов.
     *
     * @return множество серверов
     */
    public synchronized int getVirtualNodes() {
        return ring.getVirtualNodes();
    }

    /**
     * Обновляет число виртуальных узлов (решардинг).
     *
     * @param newVirtualNodes новое количество виртуальных узлов
     */
    public synchronized void updateShardCount(int newVirtualNodes) {
        ConsistentHashRing<ServerNode> oldRing = this.ring;
        this.ring = new ConsistentHashRing<>(
                new ConsistentHashRing.MD5HashFunction(),
                nodes,
                newVirtualNodes
        );
        Map<ServerNode, List<HashRange>> migrationPlan = calculateMigrationRanges(oldRing, ring);
        executeRestRangeMigration(migrationPlan, oldRing);
    }

    Map<ServerNode, List<HashRange>> calculateMigrationRanges(
            ConsistentHashRing<ServerNode> oldRing,
            ConsistentHashRing<ServerNode> newRing) {
        Map<ServerNode, List<HashRange>> migrationPlan = new HashMap<>();
        for (ServerNode node : newRing.getNodes()) {
            List<HashRange> newRanges = newRing.getHashRanges(node);
            for (HashRange range : newRanges) {
                ServerNode oldOwner = oldRing.getNodeByHash(range.getStart());
                if (oldOwner == null || !oldOwner.equals(node)) {
                     migrationPlan
                            .computeIfAbsent(node, k -> new ArrayList<>())
                            .add(range);
                }
            }
        }
        return migrationPlan;
    }

    void executeRestRangeMigration(
            Map<ServerNode, List<HashRange>> migrationPlan,
            ConsistentHashRing<ServerNode> oldRing
    ) {
        for (Map.Entry<ServerNode, List<HashRange>> entry : migrationPlan.entrySet()) {
            ServerNode targetNode = entry.getKey();
            List<HashRange> rangesToMigrate = entry.getValue();
            for (HashRange range : rangesToMigrate) {
                ServerNode sourceNode = oldRing.getNodeForHash(range.getStart());
                restClient.migrateRangeDirectly(
                        sourceNode,
                        targetNode,
                        range.getStart(),
                        range.getEnd()
                );
            }
        }
    }
}
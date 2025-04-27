package team.brown.sharding.master.node;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import team.brown.sharding.master.grpc.HashRange;
import team.brown.sharding.master.grpc.RestClient;
import team.brown.sharding.master.hash.ConsistentHashRing;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Пример мастер-узла, управляющего схемой шардирования.
 */
@Slf4j
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
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

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
    public boolean addServer(ServerNode node) {
        log.info("Add server: node={}", node);
        lock.writeLock().lock();
        try {
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
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Удаляет сервер из кольца.
     *
     * @param node сервер
     * @return true, если сервер удалён; false если его не было
     */
    public synchronized boolean removeServer(ServerNode node) {
        log.info("Remove server: node={}", node);
        lock.writeLock().lock();
        try {
            if (!nodes.contains(node)) {
                return false;
            }
            nodes.remove(node);
            ConsistentHashRing<ServerNode> oldRing = this.ring.clone();
            ring.removeNode(node);
            Map<ServerNode, List<HashRange>> migrationPlan = calculateMigrationRanges(oldRing, ring);
            executeRestRangeMigration(migrationPlan, oldRing);
            return true;
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Возвращает сервер, ответственный за данный ключ.
     *
     * @param key ключ
     * @return сервер
     */
    public ServerNode getServerForKey(String key) {
        log.info("Get server for key: key={}", key);
        lock.readLock().lock();
        try {
            return ring.getNode(key);
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Возвращает текущее множество серверов.
     *
     * @return множество серверов
     */
    public Set<ServerNode> getNodes() {
        log.info("Get all nodes");
        lock.readLock().lock();
        try {
            return new HashSet<>(nodes);
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Возвращает текущее множество серверов.
     *
     * @return множество серверов
     */
    public int getVirtualNodes() {
        log.info("Get virtual nodes count");
        lock.readLock().lock();
        try {
            return ring.getVirtualNodes();
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Обновляет число виртуальных узлов (решардинг).
     *
     * @param newVirtualNodes новое количество виртуальных узлов
     */
    public void updateShardCount(int newVirtualNodes) {
        log.info("Update shard count: newVirtualNodes={}", newVirtualNodes);
        lock.writeLock().lock();
        try {
            ConsistentHashRing<ServerNode> oldRing = this.ring;
            this.ring = new ConsistentHashRing<>(
                    new ConsistentHashRing.MD5HashFunction(),
                    nodes,
                    newVirtualNodes
            );
            Map<ServerNode, List<HashRange>> migrationPlan = calculateMigrationRanges(oldRing, ring);
            executeRestRangeMigration(migrationPlan, oldRing);
        } finally {
            lock.writeLock().unlock();
        }
    }

    Map<ServerNode, List<HashRange>> calculateMigrationRanges(
            ConsistentHashRing<ServerNode> oldRing,
            ConsistentHashRing<ServerNode> newRing) {
        Map<ServerNode, List<HashRange>> migrationPlan = new HashMap<>();
        for (ServerNode node : newRing.getNodes()) {
            List<HashRange> newRanges = newRing.getNodes().size() == 1
                    ? List.of(new HashRange(Integer.MIN_VALUE, Integer.MAX_VALUE))
                    : newRing.getHashRanges(node);
            log.info("for node {} calculating new ranges {}", node.getAddress(), newRanges);
            for (HashRange range : newRanges) {
                ServerNode oldOwner = oldRing.getNodeByHash(range.getStart());
                log.info("for newRange {} found old owner {}", range, oldOwner);
                if (oldOwner == null || !oldOwner.equals(node)) {
                     migrationPlan
                            .computeIfAbsent(node, k -> new ArrayList<>())
                            .add(range);
                }
            }
        }
        log.info("Calculated new migrationPlan: {}", migrationPlan);
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
                log.info("Call migration from {} to {}", sourceNode, targetNode);
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
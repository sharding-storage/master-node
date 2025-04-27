package team.brown.sharding.master.grpc;

import team.brown.sharding.master.node.ServerNode;

public interface RestClient {
    void migrateRangeDirectly(ServerNode sourceNode, ServerNode targetNode, int start, int end, int version);
}
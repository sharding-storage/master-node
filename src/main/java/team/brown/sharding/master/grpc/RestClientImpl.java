package team.brown.sharding.master.grpc;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import team.brown.sharding.master.model.MigrationRequest;
import team.brown.sharding.master.node.ServerNode;

@Component
public class RestClientImpl implements RestClient {
    private final RestTemplate restTemplate;
    private final String migrationEndpoint = "/storage/direct";

    public RestClientImpl(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Override
    public void migrateRangeDirectly(ServerNode sourceNode,
                                     ServerNode targetNode,
                                     int startHash,
                                     int endHash) {
        String url = buildMigrationUrl(sourceNode);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        MigrationRequest request = new MigrationRequest(
                targetNode.getAddress(),
                (long) startHash,
                (long) endHash
        );

        HttpEntity<MigrationRequest> entity = new HttpEntity<>(request, headers);

        restTemplate.exchange(
                url,
                HttpMethod.POST,
                entity,
                Void.class
        );
    }


    private String buildMigrationUrl(ServerNode node) {
        return String.format("http://%s%s",
                node.getAddress(),
                migrationEndpoint);
    }

}
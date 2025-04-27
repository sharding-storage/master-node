package team.brown.sharding.master.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import team.brown.sharding.master.model.ChangeShardRequest;
import team.brown.sharding.master.model.CommonResponse;
import team.brown.sharding.master.model.NodeRequest;
import team.brown.sharding.master.model.SchemaResponse;
import team.brown.sharding.master.node.MasterNode;
import team.brown.sharding.master.node.ServerNode;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Контроллер для управления мастер-узлом и схемой шардирования.
 */
@Slf4j
@RestController
@Tag(name = "main", description = "API мастер-узла")
@RequiredArgsConstructor
public class MasterController {

    // Экземпляр мастер-узла, управляющего шардированием
    private final MasterNode masterNode;

    /**
     * Возвращает схему узлов (адреса) в порядке хеширования.
     *
     * @return схема узлов
     */
    @Operation(summary = "Обновить схему", description = "Получить схему узлов")
    @GetMapping("/scheme")
    public SchemaResponse refreshSchema() {
        log.info("Refresh schema");
        List<String> nodes = masterNode.getNodes().stream()
                .map(ServerNode::getAddress)
                .collect(Collectors.toList());
        return new SchemaResponse(nodes, masterNode.getVirtualNodes());
    }

    /**
     * Добавляет новый узел в пул.
     *
     * @param request запрос с информацией об узле
     * @return ответ с сообщением
     */
    @Operation(summary = "Добавить узел", description = "Добавить новый узел в шардирующий пул")
    @PostMapping("/scheme")
    public CommonResponse addNode(@RequestBody @Valid NodeRequest request) {
        log.info("Add node: request={}", request);
        ServerNode node = new ServerNode(request.address());
        boolean added = masterNode.addServer(node);
        return new CommonResponse(added ? "done" : "node already exists");
    }

    /**
     * Удаляет узел из пула по его адресу.
     *
     * @param server адрес узла для удаления
     * @return ответ с сообщением
     */
    @Operation(summary = "Удалить узел", description = "Удалить узел из шардирующего пула")
    @DeleteMapping("/scheme/{server}")
    public CommonResponse removeNode(@PathVariable("server") String server) {
        log.info("Remove node: server={}", server);
        ServerNode node = new ServerNode(server);
        boolean removed = masterNode.removeServer(node);
        return new CommonResponse(removed ? "done" : "node not found");
    }

    /**
     * Обновляет число шардов (виртуальных узлов) в кластере.
     * @param request запрос с новым количеством шардов
     * @return ответ с сообщением
     */
    @Operation(summary = "Обновить количество шардов", description = "Изменить число шардов в кластере")
    @PutMapping("/shards")
    public CommonResponse updateShards(@RequestBody ChangeShardRequest request) {
        log.info("Update shards: request={}", request);
        masterNode.updateShardCount(request.shardCount());
        return new CommonResponse("done");
    }
}
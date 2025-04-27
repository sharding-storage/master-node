package team.brown.sharding.master;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import team.brown.sharding.master.controller.MasterController;
import team.brown.sharding.master.model.ChangeShardRequest;
import team.brown.sharding.master.model.NodeRequest;
import team.brown.sharding.master.node.MasterNode;
import team.brown.sharding.master.node.ServerNode;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashSet;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Тесты для MasterController.
 */
@WebMvcTest(MasterController.class)
public class MasterControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MasterNode masterNode;

    @Autowired
    private ObjectMapper objectMapper;

    private ServerNode node1;
    private ServerNode node2;

    @BeforeEach
    public void setUp() {
        node1 = new ServerNode("http://192.168.1.1:8000");
        node2 = new ServerNode("http://192.168.1.2:8000");
    }

    @Test
    public void testRefreshSchema() throws Exception {
        when(masterNode.getNodes()).thenReturn(new HashSet<>(Arrays.asList(node1, node2)));

        mockMvc.perform(get("/scheme"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nodes").isArray())
                .andExpect(jsonPath("$.nodes[0]").value(node1.getAddress()));
    }

    @Test
    public void testAddNode() throws Exception {
        NodeRequest request = new NodeRequest("http://192.168.1.3:8000");
        when(masterNode.addServer(any(ServerNode.class))).thenReturn(true);

        mockMvc.perform(post("/scheme")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.answer").value("done"));
    }

    @Test
    public void testRemoveNode() throws Exception {
        when(masterNode.removeServer(any(ServerNode.class))).thenReturn(true);

        String encodedAddress = URLEncoder.encode("http://192.168.1.1:8000", StandardCharsets.UTF_8);

        mockMvc.perform(delete("/scheme/{server}", encodedAddress))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.answer").value("done"));
    }

    @Test
    public void testUpdateShards() throws Exception {
        ChangeShardRequest request = new ChangeShardRequest(4);
        mockMvc.perform(put("/shards")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.answer").value("done"));
    }
}

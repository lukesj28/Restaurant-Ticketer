package com.ticketer.integrations;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ticketer.dtos.Requests.AnalysisRequest;
import com.ticketer.models.DailyTicketLog;
import com.ticketer.models.Order;
import com.ticketer.models.OrderItem;
import com.ticketer.models.Ticket;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.io.File;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "tickets.dir=target/test-analysis-integration"
})
public class AnalysisIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper mapper;

    private static final String TEST_DIR = "target/test-analysis-integration";

    @BeforeEach
    public void setUp() {
        File testDir = new File(TEST_DIR);
        if (!testDir.exists())
            testDir.mkdirs();
    }

    @AfterEach
    public void tearDown() {
        deleteDirectory(new File(TEST_DIR));
    }

    private void deleteDirectory(File directory) {
        if (directory.exists()) {
            File[] files = directory.listFiles();
            if (files != null) {
                for (File f : files) {
                    if (f.isDirectory())
                        deleteDirectory(f);
                    else
                        f.delete();
                }
            }
            directory.delete();
        }
    }

    @Test
    public void testAnalysisWorkflow() throws Exception {
        Ticket t1 = new Ticket(1);
        t1.setCreatedAt(Instant.parse("2023-01-01T10:00:00Z"));
        t1.setClosedAt(Instant.parse("2023-01-01T10:30:00Z"));
        Order o1 = new Order(0);
        o1.addItem(new OrderItem("IntegrationBurger", "Fries", 1500));
        t1.addOrder(o1);

        List<Ticket> tickets = new ArrayList<>();
        tickets.add(t1);

        DailyTicketLog log = new DailyTicketLog(Collections.emptyMap(), tickets, 1500, 1500, 1, 1);
        mapper.writeValue(new File(TEST_DIR + "/2023-01-01.json"), log);

        AnalysisRequest request = new AnalysisRequest("2023-01-01", "2023-01-01");

        mockMvc.perform(post("/api/analysis")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.payload.totalTicketCount").value(1))
                .andExpect(jsonPath("$.payload.totalTotalCents").value(1500))
                .andExpect(jsonPath("$.payload.totalSubtotalCents").value(1500))
                .andExpect(jsonPath("$.payload.itemRankings[0].name").value("IntegrationBurger"));
    }
}

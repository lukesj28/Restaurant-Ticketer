package com.ticketer.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ticketer.models.AnalysisReport;
import com.ticketer.models.AnalysisReport.ItemRank;
import com.ticketer.models.AnalysisReport.SideRank;
import com.ticketer.models.Ticket;
import com.ticketer.models.DailyTicketLog;
import com.ticketer.models.Order;
import com.ticketer.models.OrderItem;
import com.ticketer.repositories.TicketRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class AnalysisServiceTest {

    @Mock
    private TicketRepository ticketRepository;

    private AnalysisService analysisService;
    private ObjectMapper mapper;
    private final String TEST_DIR = "target/test-analysis-service";
    private Clock clock;

    @BeforeEach
    public void setUp() {
        File testDir = new File(TEST_DIR);
        if (!testDir.exists())
            testDir.mkdirs();

        mapper = new com.ticketer.config.JacksonConfig().objectMapper();
        clock = Clock.fixed(Instant.parse("2023-01-02T12:00:00Z"), ZoneId.of("UTC"));

        analysisService = new AnalysisService(ticketRepository, mapper, TEST_DIR, clock);
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
    public void testGenerateReportBasicAggregation() throws IOException {
        List<Ticket> historicalTickets = new ArrayList<>();
        Ticket t1 = new Ticket(1);
        Order o1 = new Order(0);
        o1.addItem(new OrderItem("HistoryItem", null, 1000, 0));
        t1.addOrder(o1);
        historicalTickets.add(t1);

        DailyTicketLog log = new DailyTicketLog(Collections.emptyMap(), historicalTickets, 1000, 1000, 1, 1);
        mapper.writeValue(new File(TEST_DIR + "/2023-01-01.json"), log);

        List<Ticket> memoryTickets = new ArrayList<>();
        Ticket t2 = new Ticket(2);

        Order o2a = new Order(0);
        o2a.addItem(new OrderItem("MemoryItem1", null, 1000, 0));
        t2.addOrder(o2a);

        Order o2b = new Order(0);
        o2b.addItem(new OrderItem("MemoryItem2", null, 1000, 0));
        t2.addOrder(o2b);

        memoryTickets.add(t2);

        when(ticketRepository.findAllClosed()).thenReturn(memoryTickets);

        AnalysisReport report = analysisService.generateReport(
                LocalDate.of(2023, 1, 1),
                LocalDate.of(2023, 1, 2));

        assertEquals(2, report.getTotalTicketCount());
        assertEquals(3, report.getTotalOrderCount());
        assertEquals(2, report.getTotalTicketCount());
        assertEquals(3, report.getTotalOrderCount());
        assertEquals(3000, report.getTotalTotalCents());
        assertEquals(1500, report.getAverageTicketTotalCents());
    }

    @Test
    public void testAverageRounding() throws IOException {
        List<Ticket> tickets = new ArrayList<>();
        Ticket t1 = new Ticket(1);
        Order o1 = new Order(0);
        o1.addItem(new OrderItem("Item1", "none", 100, 0));
        t1.addOrder(o1);

        Ticket t2 = new Ticket(2);
        Order o2 = new Order(0);
        o2.addItem(new OrderItem("Item2", "none", 101, 0));
        t2.addOrder(o2);

        tickets.add(t1);
        tickets.add(t2);

        DailyTicketLog log = new DailyTicketLog(Collections.emptyMap(), tickets, 201, 201, 2, 2);
        mapper.writeValue(new File(TEST_DIR + "/2023-01-01.json"), log);

        AnalysisReport report = analysisService.generateReport(LocalDate.of(2023, 1, 1), LocalDate.of(2023, 1, 1));
        assertEquals(101, report.getAverageTicketTotalCents());
    }

    @Test
    public void testHeatmapAndTimezone() throws IOException {
        Ticket t1 = new Ticket(1);
        ZoneId zone = ZoneId.systemDefault();

        java.time.ZonedDateTime local10am = java.time.ZonedDateTime.of(2023, 1, 1, 10, 0, 0, 0, zone);
        t1.setCreatedAt(local10am.toInstant());

        java.time.ZonedDateTime local3pm = java.time.ZonedDateTime.of(2023, 1, 1, 15, 0, 0, 0, zone);
        Ticket t2 = new Ticket(2);
        t2.setCreatedAt(local3pm.toInstant());

        List<Ticket> tickets = new ArrayList<>();
        tickets.add(t1);
        tickets.add(t2);

        DailyTicketLog log = new DailyTicketLog(Collections.emptyMap(), tickets, 0, 0, 2, 0);
        mapper.writeValue(new File(TEST_DIR + "/2023-01-01.json"), log);

        AnalysisReport report = analysisService.generateReport(LocalDate.of(2023, 1, 1), LocalDate.of(2023, 1, 1));

        Map<Integer, Integer> heatmap = report.getHourlyTraffic();
        assertEquals(1, heatmap.get(10));
        assertEquals(0, heatmap.get(12));
    }

    @Test
    public void testTurnoverCalculation() throws IOException {
        Ticket t1 = new Ticket(1);
        t1.setCreatedAt(Instant.parse("2023-01-01T10:00:00Z"));
        t1.setClosedAt(Instant.parse("2023-01-01T10:30:00Z"));

        Ticket t2 = new Ticket(2);
        t2.setCreatedAt(Instant.parse("2023-01-01T11:00:00Z"));
        t2.setClosedAt(Instant.parse("2023-01-01T12:00:00Z"));

        Ticket t3 = new Ticket(3);
        t3.setCreatedAt(Instant.parse("2023-01-01T13:00:00Z"));
        t3.setClosedAt(null);

        List<Ticket> tickets = new ArrayList<>();
        tickets.add(t1);
        tickets.add(t2);
        tickets.add(t3);

        DailyTicketLog log = new DailyTicketLog(Collections.emptyMap(), tickets, 0, 0, 3, 0);
        mapper.writeValue(new File(TEST_DIR + "/2023-01-01.json"), log);

        AnalysisReport report = analysisService.generateReport(LocalDate.of(2023, 1, 1), LocalDate.of(2023, 1, 1));

        assertEquals(45, report.getAverageTurnoverTimeMinutes());
    }

    @Test
    public void testRankings() throws IOException {
        Ticket t1 = new Ticket(1);
        Order o1 = new Order(0);
        o1.addItem(new OrderItem("Burger", "Fries", 1000, 200));
        o1.addItem(new OrderItem("Burger", "Salad", 1000, 200));
        o1.addItem(new OrderItem("Soda", "none", 200, 0));
        t1.addOrder(o1);

        List<Ticket> tickets = new ArrayList<>();
        tickets.add(t1);

        DailyTicketLog log = new DailyTicketLog(Collections.emptyMap(), tickets, 0, 0, 1, 0);
        mapper.writeValue(new File(TEST_DIR + "/2023-01-01.json"), log);

        AnalysisReport report = analysisService.generateReport(LocalDate.of(2023, 1, 1), LocalDate.of(2023, 1, 1));

        List<ItemRank> items = report.getItemRankings();
        assertEquals(4, items.size());

        assertEquals("Burger", items.get(0).getName());
        assertEquals(2, items.get(0).getCount());
        assertEquals(2000, items.get(0).getTotalRevenueCents());

        ItemRank fries = items.stream().filter(i -> i.getName().equals("Fries")).findFirst().orElse(null);
        assertNotNull(fries);
        assertEquals(1, fries.getCount());
        assertEquals(200, fries.getTotalRevenueCents());

        ItemRank salad = items.stream().filter(i -> i.getName().equals("Salad")).findFirst().orElse(null);
        assertNotNull(salad);
        assertEquals(1, salad.getCount());
        assertEquals(200, salad.getTotalRevenueCents());

        Map<String, List<SideRank>> sideRankings = report.getSideRankings();

        List<SideRank> burgerSides = sideRankings.get("Burger");
        assertNotNull(burgerSides);
        assertEquals(2, burgerSides.size());
        assertTrue(burgerSides.stream().anyMatch(s -> s.getName().equals("Fries") && s.getCount() == 1));
        assertTrue(burgerSides.stream().anyMatch(s -> s.getName().equals("Salad") && s.getCount() == 1));
    }

    @Test
    public void testDailyRanking() {
        Ticket t1 = new Ticket(1);
        t1.setCreatedAt(Instant.parse("2023-01-01T10:00:00Z"));
        Order o1 = new Order(0);
        o1.addItem(new OrderItem("Item1", "none", 1000, 0));
        t1.addOrder(o1);

        Ticket t2 = new Ticket(2);
        t2.setCreatedAt(Instant.parse("2023-01-02T10:00:00Z"));
        Order o2 = new Order(0);
        o2.addItem(new OrderItem("Item1", "none", 2000, 0));
        t2.addOrder(o2);

        Ticket t3 = new Ticket(3);
        t3.setCreatedAt(Instant.parse("2023-01-03T10:00:00Z"));
        Order o3 = new Order(0);
        o3.addItem(new OrderItem("Item1", "none", 500, 0));
        t3.addOrder(o3);

        DailyTicketLog log1 = new DailyTicketLog(Collections.emptyMap(), List.of(t1), 1000, 1000, 1, 1);
        DailyTicketLog log2 = new DailyTicketLog(Collections.emptyMap(), List.of(t2), 2000, 2000, 1, 1);
        DailyTicketLog log3 = new DailyTicketLog(Collections.emptyMap(), List.of(t3), 500, 500, 1, 1);

        try {
            mapper.writeValue(new File(TEST_DIR + "/2023-01-01.json"), log1);
            mapper.writeValue(new File(TEST_DIR + "/2023-01-02.json"), log2);
            mapper.writeValue(new File(TEST_DIR + "/2023-01-03.json"), log3);
        } catch (IOException e) {
            fail("Failed to write daily logs");
        }

        AnalysisReport report = analysisService.generateReport(LocalDate.of(2023, 1, 1), LocalDate.of(2023, 1, 3));

        assertEquals(3, report.getDayRankings().size());
        assertEquals("2023-01-02", report.getDayRankings().get(0).getDate());
        assertEquals("2023-01-01", report.getDayRankings().get(1).getDate());
        assertEquals("2023-01-03", report.getDayRankings().get(2).getDate());
        assertEquals(2000, report.getDayRankings().get(0).getTotalTotalCents());
    }

    @Test
    public void testPersistedTotals() throws IOException {
        Ticket t1 = new Ticket(1);
        Order o1 = new Order(0);
        o1.addItem(new OrderItem("Item1", "none", 1000, 0));
        t1.addOrder(o1);

        List<Ticket> tickets = new ArrayList<>();
        tickets.add(t1);

        String json = "{\"tickets\":[{\"id\":1,\"subtotal\":1000,\"total\":1100,\"orders\":[{\"items\":[{\"name\":\"Item1\",\"mainPrice\":1000,\"sidePrice\":0}]}]}]}";

        File dailyFile = new File(TEST_DIR + "/2023-01-01.json");
        com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        mapper.writeValue(dailyFile, mapper.readTree(json));

        AnalysisReport report = analysisService.generateReport(LocalDate.of(2023, 1, 1), LocalDate.of(2023, 1, 1));

        assertEquals(1100, report.getTotalTotalCents());
        assertEquals(1000, report.getTotalSubtotalCents());
    }
}

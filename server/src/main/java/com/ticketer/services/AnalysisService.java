package com.ticketer.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ticketer.models.AnalysisReport;
import com.ticketer.models.AnalysisReport.ItemRank;
import com.ticketer.models.AnalysisReport.SideRank;
import com.ticketer.models.DailyTicketLog;
import com.ticketer.models.Ticket;
import com.ticketer.repositories.TicketRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Comparator;
import java.util.stream.Collectors;
import com.ticketer.models.AnalysisReport.DayRank;

@Service
public class AnalysisService {

    private static final Logger logger = LoggerFactory.getLogger(AnalysisService.class);

    private final TicketRepository ticketRepository;
    private final ObjectMapper mapper;
    private final String ticketsDir;
    private final java.time.Clock clock;

    public AnalysisService(TicketRepository ticketRepository,
            ObjectMapper mapper,
            @Value("${tickets.dir:data/tickets}") String ticketsDir,
            java.time.Clock clock) {
        this.ticketRepository = ticketRepository;
        this.mapper = mapper;
        this.ticketsDir = ticketsDir;
        this.clock = clock;
    }

    public AnalysisReport generateReport(LocalDate startDate, LocalDate endDate) {
        logger.info("Generating analysis report from {} to {}", startDate, endDate);

        List<Ticket> allTickets = new ArrayList<>();
        int totalTicketCount = 0;
        int totalOrderCount = 0;
        int totalSubtotalCents = 0;
        int totalTotalCents = 0;

        LocalDate current = startDate;
        while (!current.isAfter(endDate)) {
            File dailyFile = new File(ticketsDir, current.toString() + ".json");
            if (dailyFile.exists()) {
                try {
                    DailyTicketLog log = mapper.readValue(dailyFile, DailyTicketLog.class);
                    allTickets.addAll(log.getTickets());

                } catch (IOException e) {
                    logger.error("Failed to read ticket log for {}", current, e);
                }
            }
            current = current.plusDays(1);
        }

        LocalDate today = LocalDate.now(clock.withZone(ZoneId.systemDefault()));
        if (!today.isBefore(startDate) && !today.isAfter(endDate)) {
            List<Ticket> inMemory = ticketRepository.findAllClosed();
            allTickets.addAll(inMemory);
        }

        AnalysisReport report = new AnalysisReport();
        report.setStartDate(startDate.toString());
        report.setEndDate(endDate.toString());

        totalTicketCount = allTickets.size();

        totalTicketCount = allTickets.size();
        totalSubtotalCents = allTickets.stream().mapToInt(Ticket::getSubtotal).sum();
        totalTotalCents = allTickets.stream().mapToInt(Ticket::getTotal).sum();
        totalOrderCount = allTickets.stream().mapToInt(t -> t.getOrders().size()).sum();

        report.setTotalTicketCount(totalTicketCount);
        report.setTotalOrderCount(totalOrderCount);
        report.setTotalSubtotalCents(totalSubtotalCents);
        report.setTotalTotalCents(totalTotalCents);

        if (totalTicketCount > 0) {
            report.setAverageTicketSubtotalCents((int) Math.round((double) totalSubtotalCents / totalTicketCount));
            report.setAverageTicketTotalCents((int) Math.round((double) totalTotalCents / totalTicketCount));
        } else {
            report.setAverageTicketSubtotalCents(0);
            report.setAverageTicketTotalCents(0);
        }

        Map<Integer, Integer> hourlyTraffic = new java.util.HashMap<>();
        for (int i = 0; i < 24; i++) {
            hourlyTraffic.put(i, 0);
        }

        ZoneId zoneId = ZoneId.systemDefault();
        for (Ticket t : allTickets) {
            java.time.ZonedDateTime zdt = t.getCreatedAt().atZone(zoneId);
            int hour = zdt.getHour();
            hourlyTraffic.merge(hour, 1, Integer::sum);
        }
        report.setHourlyTraffic(hourlyTraffic);

        double totalDurationSeconds = 0;
        int turnoverCount = 0;

        for (Ticket t : allTickets) {
            if (t.getClosedAt() != null) {
                java.time.Duration duration = java.time.Duration.between(t.getCreatedAt(), t.getClosedAt());
                totalDurationSeconds += duration.getSeconds();
                turnoverCount++;
            }
        }

        if (turnoverCount > 0) {
            double avgSeconds = totalDurationSeconds / turnoverCount;
            report.setAverageTurnoverTimeMinutes((int) Math.round(avgSeconds / 60.0));
        } else {
            report.setAverageTurnoverTimeMinutes(0);
        }

        Map<String, ItemRank> itemMap = new java.util.HashMap<>();
        Map<String, SideRank> sideMap = new java.util.HashMap<>();

        for (Ticket t : allTickets) {
            for (com.ticketer.models.Order o : t.getOrders()) {
                for (com.ticketer.models.OrderItem item : o.getItems()) {
                    itemMap.putIfAbsent(item.getName(),
                            new ItemRank(item.getName(), 0, 0));
                    ItemRank itemRank = itemMap.get(item.getName());
                    itemRank.setCount(itemRank.getCount() + 1);
                    itemRank.setTotalRevenueCents(itemRank.getTotalRevenueCents() + item.getMainPrice());

                    String side = item.getSelectedSide();
                    if (side != null && !side.isEmpty() && !"none".equalsIgnoreCase(side)) {
                        sideMap.putIfAbsent(side, new SideRank(side, 0));
                        SideRank sideRank = sideMap.get(side);
                        sideRank.setCount(sideRank.getCount() + 1);
                    }
                }
            }
        }

        List<ItemRank> sortedItems = new ArrayList<>(itemMap.values());
        sortedItems.sort((a, b) -> b.getCount() - a.getCount());
        report.setItemRankings(sortedItems);

        List<SideRank> sortedSides = new ArrayList<>(sideMap.values());
        sortedSides.sort((a, b) -> b.getCount() - a.getCount());
        report.setSideRankings(sortedSides);

        Map<String, Integer> dailyTotals = allTickets.stream()
                .collect(Collectors.groupingBy(
                        t -> t.getCreatedAt().atZone(zoneId).toLocalDate().toString(),
                        Collectors.summingInt(Ticket::getTotal)));

        List<DayRank> dayRankings = dailyTotals.entrySet().stream()
                .map(entry -> new DayRank(entry.getKey(), entry.getValue()))
                .sorted(Comparator.comparingInt(DayRank::getTotalTotalCents).reversed())
                .toList();

        report.setDayRankings(dayRankings);

        return report;
    }
}

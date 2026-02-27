package com.ticketer.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ticketer.models.AnalysisReport;
import com.ticketer.models.AnalysisReport.ItemRank;
import com.ticketer.models.AnalysisReport.SideRank;
import com.ticketer.models.ComboComponentSnapshot;
import com.ticketer.models.ComboSlotSelection;
import com.ticketer.models.DailyTicketLog;
import com.ticketer.models.OrderItem;
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
import java.util.Comparator;
import java.util.List;
import java.util.Map;
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
            allTickets.addAll(ticketRepository.findAllClosed());
        }

        AnalysisReport report = new AnalysisReport();
        report.setStartDate(startDate.toString());
        report.setEndDate(endDate.toString());

        int totalTicketCount = allTickets.size();
        long totalSubtotalCents = allTickets.stream().mapToLong(Ticket::getSubtotal).sum();
        long totalTotalCents = allTickets.stream().mapToLong(Ticket::getTotal).sum();
        int totalOrderCount = allTickets.stream().mapToInt(t -> t.getOrders().size()).sum();

        report.setTotalTicketCount(totalTicketCount);
        report.setTotalOrderCount(totalOrderCount);
        report.setTotalSubtotalCents(totalSubtotalCents);
        report.setTotalTotalCents(totalTotalCents);

        long barSubtotalCents = 0;
        long barTaxCents = 0;
        for (Ticket t : allTickets) {
            long ticketSubtotal = t.getSubtotal();
            long ticketTax = t.getTax();
            long ticketBarSubtotal = 0;
            for (com.ticketer.models.Order o : t.getOrders()) {
                for (com.ticketer.models.OrderItem item : o.getItems()) {
                    if (!item.isCombo() && item.isAlcohol()) {
                        ticketBarSubtotal += item.getPrice();
                    }
                }
            }
            barSubtotalCents += ticketBarSubtotal;
            if (ticketSubtotal > 0 && ticketTax > 0) {
                barTaxCents += Math.round((double) ticketBarSubtotal / ticketSubtotal * ticketTax);
            }
        }
        report.setBarSubtotalCents(barSubtotalCents);
        report.setBarTaxCents(barTaxCents);
        report.setBarTotalCents(barSubtotalCents + barTaxCents);

        if (totalTicketCount > 0) {
            report.setAverageTicketSubtotalCents(
                    Math.round((double) totalSubtotalCents / totalTicketCount));
            report.setAverageTicketTotalCents(
                    Math.round((double) totalTotalCents / totalTicketCount));
        } else {
            report.setAverageTicketSubtotalCents(0);
            report.setAverageTicketTotalCents(0);
        }

        Map<Integer, Integer> hourlyTraffic = new java.util.HashMap<>();
        for (int i = 0; i < 24; i++) hourlyTraffic.put(i, 0);
        ZoneId zoneId = ZoneId.systemDefault();
        for (Ticket t : allTickets) {
            java.time.ZonedDateTime zdt = t.getCreatedAt().atZone(zoneId);
            hourlyTraffic.merge(zdt.getHour(), 1, Integer::sum);
        }
        report.setHourlyTraffic(hourlyTraffic);

        double totalDurationSeconds = 0;
        int turnoverCount = 0;
        for (Ticket t : allTickets) {
            if (t.getClosedAt() != null) {
                java.time.Duration dur = java.time.Duration.between(t.getCreatedAt(), t.getClosedAt());
                totalDurationSeconds += dur.getSeconds();
                turnoverCount++;
            }
        }
        if (turnoverCount > 0) {
            report.setAverageTurnoverTimeMinutes((int) Math.round(totalDurationSeconds / turnoverCount / 60.0));
        } else {
            report.setAverageTurnoverTimeMinutes(0);
        }

        Map<String, ItemRank> itemMap = new java.util.HashMap<>();
        Map<String, Map<String, Integer>> itemSideCounts = new java.util.HashMap<>();

        for (Ticket t : allTickets) {
            for (com.ticketer.models.Order o : t.getOrders()) {
                for (OrderItem item : o.getItems()) {
                    if (item.isCombo()) {
                        trackItem(itemMap, item.getName(), item.getMainPrice());

                        if (item.getComponents() != null) {
                            for (ComboComponentSnapshot comp : item.getComponents()) {
                                trackItem(itemMap, comp.getName(), 0);
                            }
                        }

                        if (item.getSlotSelections() != null) {
                            for (ComboSlotSelection sel : item.getSlotSelections()) {
                                trackItem(itemMap, sel.getSelectedName(), 0);
                                itemSideCounts.computeIfAbsent(item.getName(), k -> new java.util.HashMap<>())
                                        .merge(sel.getSelectedName(), 1, Integer::sum);
                            }
                        }
                    } else {
                        trackItem(itemMap, item.getName(), item.getMainPrice());

                        String side = item.getSelectedSide();
                        if (side != null && !side.isEmpty()) {
                            itemSideCounts.computeIfAbsent(item.getName(), k -> new java.util.HashMap<>())
                                    .merge(side, 1, Integer::sum);
                            trackItem(itemMap, side, item.getSidePrice());
                        }
                    }
                }
            }
        }

        List<ItemRank> sortedItems = new ArrayList<>(itemMap.values());
        sortedItems.sort((a, b) -> b.getCount() - a.getCount());
        report.setItemRankings(sortedItems);

        Map<String, List<SideRank>> finalSideRankings = new java.util.HashMap<>();
        for (Map.Entry<String, Map<String, Integer>> entry : itemSideCounts.entrySet()) {
            List<SideRank> ranks = entry.getValue().entrySet().stream()
                    .map(e -> new SideRank(e.getKey(), e.getValue()))
                    .sorted((a, b) -> b.getCount() - a.getCount())
                    .collect(Collectors.toList());
            finalSideRankings.put(entry.getKey(), ranks);
        }
        report.setSideRankings(finalSideRankings);

        Map<String, Long> dailyTotals = allTickets.stream()
                .collect(Collectors.groupingBy(
                        t -> t.getCreatedAt().atZone(zoneId).toLocalDate().toString(),
                        Collectors.summingLong(Ticket::getTotal)));
        List<DayRank> dayRankings = dailyTotals.entrySet().stream()
                .map(entry -> new DayRank(entry.getKey(), entry.getValue()))
                .sorted(Comparator.comparingLong(DayRank::getTotalTotalCents).reversed())
                .toList();
        report.setDayRankings(dayRankings);

        return report;
    }

    private void trackItem(Map<String, ItemRank> itemMap, String name, long revenueCents) {
        if (name == null || name.isEmpty()) return;
        itemMap.computeIfAbsent(name, n -> new ItemRank(n, 0, 0));
        ItemRank rank = itemMap.get(name);
        rank.setCount(rank.getCount() + 1);
        rank.setTotalRevenueCents(rank.getTotalRevenueCents() + revenueCents);
    }
}

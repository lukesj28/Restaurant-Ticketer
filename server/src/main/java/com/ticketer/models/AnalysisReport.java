package com.ticketer.models;

import java.util.List;
import java.util.Map;

public class AnalysisReport {
    private String startDate;
    private String endDate;
    private int totalTicketCount;
    private int totalOrderCount;
    private int totalSubtotalCents;
    private int totalTotalCents;
    private int averageTicketSubtotalCents;
    private int averageTicketTotalCents;
    private int averageTurnoverTimeMinutes;
    private Map<Integer, Integer> hourlyTraffic;
    private List<ItemRank> itemRankings;
    private Map<String, List<SideRank>> sideRankings;
    private List<DayRank> dayRankings;

    public AnalysisReport() {
    }

    public String getStartDate() {
        return startDate;
    }

    public void setStartDate(String startDate) {
        this.startDate = startDate;
    }

    public String getEndDate() {
        return endDate;
    }

    public void setEndDate(String endDate) {
        this.endDate = endDate;
    }

    public int getTotalTicketCount() {
        return totalTicketCount;
    }

    public void setTotalTicketCount(int totalTicketCount) {
        this.totalTicketCount = totalTicketCount;
    }

    public int getTotalOrderCount() {
        return totalOrderCount;
    }

    public void setTotalOrderCount(int totalOrderCount) {
        this.totalOrderCount = totalOrderCount;
    }

    public int getTotalSubtotalCents() {
        return totalSubtotalCents;
    }

    public void setTotalSubtotalCents(int totalSubtotalCents) {
        this.totalSubtotalCents = totalSubtotalCents;
    }

    public int getTotalTotalCents() {
        return totalTotalCents;
    }

    public void setTotalTotalCents(int totalTotalCents) {
        this.totalTotalCents = totalTotalCents;
    }

    public int getAverageTicketSubtotalCents() {
        return averageTicketSubtotalCents;
    }

    public void setAverageTicketSubtotalCents(int averageTicketSubtotalCents) {
        this.averageTicketSubtotalCents = averageTicketSubtotalCents;
    }

    public int getAverageTicketTotalCents() {
        return averageTicketTotalCents;
    }

    public void setAverageTicketTotalCents(int averageTicketTotalCents) {
        this.averageTicketTotalCents = averageTicketTotalCents;
    }

    public int getAverageTurnoverTimeMinutes() {
        return averageTurnoverTimeMinutes;
    }

    public void setAverageTurnoverTimeMinutes(int averageTurnoverTimeMinutes) {
        this.averageTurnoverTimeMinutes = averageTurnoverTimeMinutes;
    }

    public Map<Integer, Integer> getHourlyTraffic() {
        return hourlyTraffic;
    }

    public void setHourlyTraffic(Map<Integer, Integer> hourlyTraffic) {
        this.hourlyTraffic = hourlyTraffic;
    }

    public List<ItemRank> getItemRankings() {
        return itemRankings;
    }

    public void setItemRankings(List<ItemRank> itemRankings) {
        this.itemRankings = itemRankings;
    }

    public Map<String, List<SideRank>> getSideRankings() {
        return sideRankings;
    }

    public void setSideRankings(Map<String, List<SideRank>> sideRankings) {
        this.sideRankings = sideRankings;
    }

    public List<DayRank> getDayRankings() {
        return dayRankings;
    }

    public void setDayRankings(List<DayRank> dayRankings) {
        this.dayRankings = dayRankings;
    }

    public static class DayRank {
        private String date;
        private int totalTotalCents;

        public DayRank(String date, int totalTotalCents) {
            this.date = date;
            this.totalTotalCents = totalTotalCents;
        }

        public String getDate() {
            return date;
        }

        public int getTotalTotalCents() {
            return totalTotalCents;
        }
    }

    public static class ItemRank {
        private String name;
        private int count;
        private int totalRevenueCents;

        public ItemRank() {
        }

        public ItemRank(String name, int count, int totalRevenueCents) {
            this.name = name;
            this.count = count;
            this.totalRevenueCents = totalRevenueCents;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public int getCount() {
            return count;
        }

        public void setCount(int count) {
            this.count = count;
        }

        public int getTotalRevenueCents() {
            return totalRevenueCents;
        }

        public void setTotalRevenueCents(int totalRevenueCents) {
            this.totalRevenueCents = totalRevenueCents;
        }
    }

    public static class SideRank {
        private String name;
        private int count;

        public SideRank() {
        }

        public SideRank(String name, int count) {
            this.name = name;
            this.count = count;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public int getCount() {
            return count;
        }

        public void setCount(int count) {
            this.count = count;
        }
    }
}

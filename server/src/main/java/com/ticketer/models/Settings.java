package com.ticketer.models;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class Settings {
    private int taxBasisPoints;
    private Map<String, String> hours;

    private PrinterSettings printer;
    private RestaurantDetails restaurant;
    private ReceiptSettings receipt;

    @JsonCreator
    public Settings(@JsonProperty("tax") int taxBasisPoints,
            @JsonProperty("hours") Map<String, String> hours,
            @JsonProperty("printer") PrinterSettings printer,
            @JsonProperty("restaurant") RestaurantDetails restaurant,
            @JsonProperty("receipt") ReceiptSettings receipt) {
        this.taxBasisPoints = taxBasisPoints;
        this.hours = hours;
        this.printer = printer != null ? printer : new PrinterSettings();
        this.restaurant = restaurant != null ? restaurant : new RestaurantDetails();
        this.receipt = receipt != null ? receipt : ReceiptSettings.defaultLayout();
    }

    public int getTax() {
        return taxBasisPoints;
    }

    public Map<String, String> getHours() {
        return hours;
    }

    public PrinterSettings getPrinter() {
        return printer;
    }

    public RestaurantDetails getRestaurant() {
        return restaurant;
    }

    public ReceiptSettings getReceipt() {
        return receipt;
    }

    public static class PrinterSettings {
        private String portName;
        private int baudRate;
        private int paperWidthMm;
        private int dpi;
        private boolean enabled;

        public PrinterSettings() {
            this.portName = "";
            this.baudRate = 38400;
            this.paperWidthMm = 80;
            this.dpi = 203;
            this.enabled = false;
        }

        @JsonCreator
        public PrinterSettings(@JsonProperty("portName") String portName,
                @JsonProperty("baudRate") int baudRate,
                @JsonProperty("paperWidthMm") int paperWidthMm,
                @JsonProperty("dpi") int dpi,
                @JsonProperty("enabled") boolean enabled) {
            this.portName = portName;
            this.baudRate = baudRate;
            this.paperWidthMm = paperWidthMm;
            this.dpi = dpi;
            this.enabled = enabled;
        }

        public String getPortName() {
            return portName;
        }

        public int getBaudRate() {
            return baudRate;
        }

        public int getPaperWidthMm() {
            return paperWidthMm;
        }

        public int getDpi() {
            return dpi;
        }

        public boolean isEnabled() {
            return enabled;
        }
    }

    public static class RestaurantDetails {
        private String name;
        private String address;
        private String phone;

        public RestaurantDetails() {
            this.name = "";
            this.address = "";
            this.phone = "";
        }

        @JsonCreator
        public RestaurantDetails(@JsonProperty("name") String name,
                @JsonProperty("address") String address,
                @JsonProperty("phone") String phone) {
            this.name = name != null ? name : "";
            this.address = address != null ? address : "";
            this.phone = phone != null ? phone : "";
        }

        public String getName() {
            return name;
        }

        public String getAddress() {
            return address;
        }

        public String getPhone() {
            return phone;
        }
    }

    public static class ReceiptBlock {
        private String type;
        private String content;

        public ReceiptBlock() {
        }

        @JsonCreator
        public ReceiptBlock(@JsonProperty("type") String type,
                @JsonProperty("content") String content) {
            this.type = type;
            this.content = content;
        }

        public String getType() {
            return type;
        }

        public String getContent() {
            return content;
        }
    }

    public static class ReceiptSettings {
        private List<ReceiptBlock> blocks;

        public ReceiptSettings() {
            this.blocks = new ArrayList<>();
        }

        @JsonCreator
        public ReceiptSettings(@JsonProperty("blocks") List<ReceiptBlock> blocks) {
            this.blocks = blocks != null ? blocks : new ArrayList<>();
        }

        public List<ReceiptBlock> getBlocks() {
            return blocks;
        }

        public static ReceiptSettings defaultLayout() {
            return new ReceiptSettings(new ArrayList<>(Arrays.asList(
                    new ReceiptBlock("RESTAURANT_NAME", null),
                    new ReceiptBlock("ADDRESS", null),
                    new ReceiptBlock("PHONE", null),
                    new ReceiptBlock("DIVIDER", null),
                    new ReceiptBlock("TIMESTAMP", null),
                    new ReceiptBlock("TABLE_NUMBER", null),
                    new ReceiptBlock("DIVIDER", null),
                    new ReceiptBlock("ITEMS", null),
                    new ReceiptBlock("SPACE", null),
                    new ReceiptBlock("DIVIDER", null),
                    new ReceiptBlock("TOTALS", null),
                    new ReceiptBlock("DIVIDER", null),
                    new ReceiptBlock("SPACE", null),
                    new ReceiptBlock("CUSTOM_TEXT", "Thank you!"))));
        }
    }
}

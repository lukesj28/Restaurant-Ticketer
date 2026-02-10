package com.ticketer.models;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class Settings {
    private int taxBasisPoints;
    private Map<String, String> hours;

    private PrinterSettings printer;

    @JsonCreator
    public Settings(@JsonProperty("tax") int taxBasisPoints,
            @JsonProperty("hours") Map<String, String> hours,
            @JsonProperty("printer") PrinterSettings printer) {
        this.taxBasisPoints = taxBasisPoints;
        this.hours = hours;
        this.printer = printer != null ? printer : new PrinterSettings();
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
}

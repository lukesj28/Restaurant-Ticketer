package com.ticketer.components;

import com.fazecast.jSerialComm.SerialPort;
import com.ticketer.exceptions.PrinterException;

import jakarta.annotation.PreDestroy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.OutputStream;

import com.ticketer.models.Settings;
import com.ticketer.services.SettingsService;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;

@Component
public class SerialPortManager {

    private static final Logger logger = LoggerFactory.getLogger(SerialPortManager.class);

    private static final byte[] ESC_POS_INIT = { 0x1B, 0x40 };

    private static final int DATA_BITS = 8;
    private static final int STOP_BITS = SerialPort.ONE_STOP_BIT;
    private static final int PARITY = SerialPort.NO_PARITY;

    private final SettingsService settingsService;
    private SerialPort serialPort;
    private final Object portLock = new Object();

    @Autowired
    public SerialPortManager(SettingsService settingsService) {
        this.settingsService = settingsService;
        Runtime.getRuntime().addShutdownHook(new Thread(this::closePort));
    }

    public OutputStream getOutputStream() {
        synchronized (portLock) {
            ensurePortOpen();
            sendInitCommand();
            return serialPort.getOutputStream();
        }
    }

    private void ensurePortOpen() {
        if (serialPort != null && serialPort.isOpen()) {
            logger.debug("Serial port already open");
            return;
        }

        int maxRetries = 3;
        int retryDelayMs = 1000;

        for (int i = 0; i < maxRetries; i++) {
            try {
                serialPort = findPrinterPort();
                if (serialPort != null) {
                    configurePort(serialPort);
                    if (serialPort.openPort()) {
                        logger.info("Opened serial port {} at {} baud",
                                serialPort.getSystemPortName(), serialPort.getBaudRate());
                        return;
                    } else {
                        logger.warn("Failed to open port {}. Attempt {} of {}",
                                serialPort.getSystemPortName(), i + 1, maxRetries);
                    }
                } else {
                    logger.info("Printer port not found. Attempt {} of {}", i + 1, maxRetries);
                }

                if (i < maxRetries - 1) {
                    Thread.sleep(retryDelayMs);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new PrinterException("Interrupted while connecting to printer", e);
            }
        }

        throw new PrinterException(
                "Printer not found or connection failed. Available ports: " + listAvailablePorts());
    }

    private SerialPort findPrinterPort() {
        Settings.PrinterSettings printerSettings = settingsService.getSettings().getPrinter();
        String configuredPortName = printerSettings.getPortName();

        SerialPort[] ports = SerialPort.getCommPorts();

        if (ports.length == 0) {
            logger.debug("No serial ports found");
            return null;
        }

        if (configuredPortName != null && !configuredPortName.isEmpty()) {
            for (SerialPort port : ports) {
                if (port.getSystemPortName().equals(configuredPortName)) {
                    logger.info("Found configured printer port: {}", port.getSystemPortName());
                    return port;
                }
            }
            logger.warn("Configured port {} not found, falling back to auto-discovery", configuredPortName);
        }

        List<String> keywords = Arrays.asList("printer", "thermal", "receipt", "epson", "star", "pos", "bixolon",
                "citizen", "fujitsu", "zebra", "samsung", "munbyn", "rongta", "tm");

        for (SerialPort port : ports) {
            String description = port.getDescriptivePortName().toLowerCase();
            String name = port.getSystemPortName().toLowerCase();

            for (String keyword : keywords) {
                if (description.contains(keyword) || name.contains(keyword)) {
                    logger.info("Found potential printer port via keyword '{}': {} ({})", keyword,
                            port.getSystemPortName(), port.getDescriptivePortName());
                    return port;
                }
            }
        }

        return null;
    }

    private void configurePort(SerialPort port) {
        int baudRate = settingsService.getSettings().getPrinter().getBaudRate();

        port.setBaudRate(baudRate);
        port.setNumDataBits(DATA_BITS);
        port.setNumStopBits(STOP_BITS);
        port.setParity(PARITY);

        port.setFlowControl(SerialPort.FLOW_CONTROL_RTS_ENABLED | SerialPort.FLOW_CONTROL_CTS_ENABLED);

        port.setComPortTimeouts(
                SerialPort.TIMEOUT_WRITE_BLOCKING,
                0,
                2000);

        logger.info("Configured port with baud rate: {}, data bits: {}, stop bits: {}, parity: none",
                baudRate, DATA_BITS, STOP_BITS);
    }

    private void sendInitCommand() {
        if (serialPort == null || !serialPort.isOpen()) {
            throw new PrinterException("Serial port is not open");
        }

        try {
            Thread.sleep(100);

            int bytesWritten = serialPort.writeBytes(ESC_POS_INIT, ESC_POS_INIT.length);

            if (bytesWritten == ESC_POS_INIT.length) {
                logger.debug("Sent ESC/POS initialize command ({} bytes)", bytesWritten);
            } else {
                logger.warn("ESC/POS init command: wrote {} of {} bytes", bytesWritten, ESC_POS_INIT.length);
            }

            Thread.sleep(50);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.warn("Interrupted while sending init command");
        } catch (Exception e) {
            logger.warn("Failed to send ESC/POS init command: {} - continuing anyway", e.getMessage());
        }
    }

    public List<String> getAvailablePorts() {
        SerialPort[] ports = SerialPort.getCommPorts();
        if (ports.length == 0) {
            return new ArrayList<>();
        }

        List<String> keywords = Arrays.asList("printer", "print", "thermal", "receipt", "epson", "star", "pos", "usb",
                "serial",
                "bixolon", "citizen", "fujitsu", "zebra", "samsung", "munbyn", "rongta", "tm", "tsp", "mc", "srp", "mp",
                "rj", "rp", "ct", "xp");
        List<String> filteredPorts = new ArrayList<>();
        List<String> allPorts = new ArrayList<>();

        for (SerialPort port : ports) {
            String portInfo = port.getSystemPortName() + " (" + port.getDescriptivePortName() + ")";
            allPorts.add(portInfo);

            String description = port.getDescriptivePortName().toLowerCase();
            String name = port.getSystemPortName().toLowerCase();

            for (String keyword : keywords) {
                if (description.contains(keyword) || name.contains(keyword)) {
                    filteredPorts.add(portInfo);
                    break;
                }
            }
        }

        return filteredPorts.isEmpty() ? allPorts : filteredPorts;
    }

    private String listAvailablePorts() {
        return String.join(", ", getAvailablePorts());
    }

    @PreDestroy
    public void closePort() {
        synchronized (portLock) {
            if (serialPort != null && serialPort.isOpen()) {
                logger.info("Closing serial port: {}", serialPort.getSystemPortName());
                serialPort.closePort();
            }
            serialPort = null;
        }
    }

    public boolean isPrinterAvailable() {
        synchronized (portLock) {
            if (serialPort != null && serialPort.isOpen()) {
                return true;
            }
            return findPrinterPort() != null;
        }
    }

    public int getBaudRate() {
        synchronized (portLock) {
            if (serialPort != null) {
                return serialPort.getBaudRate();
            }
            return settingsService.getSettings().getPrinter().getBaudRate();
        }
    }
}

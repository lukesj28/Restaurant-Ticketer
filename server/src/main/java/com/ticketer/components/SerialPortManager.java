package com.ticketer.components;

import com.fazecast.jSerialComm.SerialPort;
import com.ticketer.exceptions.PrinterException;

import jakarta.annotation.PreDestroy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.OutputStream;

@Component
public class SerialPortManager {

    private static final Logger logger = LoggerFactory.getLogger(SerialPortManager.class);

    private static final int PREFERRED_BAUD_RATE = 38400;
    private static final int[] FALLBACK_BAUD_RATES = { 19200, 9600, 4800, 2400, 1200 };

    private static final byte[] ESC_POS_INIT = { 0x1B, 0x40 };

    private static final int DATA_BITS = 8;
    private static final int STOP_BITS = SerialPort.ONE_STOP_BIT;
    private static final int PARITY = SerialPort.NO_PARITY;
    private static final int TIMEOUT_MS = 5000;

    private final String printerPortName;
    private SerialPort serialPort;
    private final Object portLock = new Object();

    public SerialPortManager() {
        this.printerPortName = "TM-m30II_020940";
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
        SerialPort[] ports = SerialPort.getCommPorts();

        if (ports.length > 0) {
            StringBuilder sb = new StringBuilder("Available ports: ");
            for (SerialPort port : ports) {
                sb.append(port.getSystemPortName())
                        .append(" (").append(port.getDescriptivePortName()).append("), ");
            }
            logger.debug(sb.toString());
        } else {
            logger.debug("No serial ports found");
        }

        for (SerialPort port : ports) {
            String portName = port.getSystemPortName();
            String description = port.getDescriptivePortName();

            if (portName.contains(printerPortName) || description.contains(printerPortName)) {
                logger.info("Found printer port: {} ({})", portName, description);
                return port;
            }

            if (description.toUpperCase().contains("EPSON") || description.toUpperCase().contains("TM-M30")) {
                logger.info("Found potential Epson printer port: {} ({})", portName, description);
                return port;
            }
        }

        return null;
    }

    private void configurePort(SerialPort port) {
        port.setBaudRate(PREFERRED_BAUD_RATE);
        port.setNumDataBits(DATA_BITS);
        port.setNumStopBits(STOP_BITS);
        port.setParity(PARITY);

        port.setFlowControl(SerialPort.FLOW_CONTROL_RTS_ENABLED | SerialPort.FLOW_CONTROL_CTS_ENABLED);

        port.setComPortTimeouts(
                SerialPort.TIMEOUT_WRITE_BLOCKING,
                0,
                2000);

        logger.info("Configured port with baud rate: {}, data bits: {}, stop bits: {}, parity: none",
                PREFERRED_BAUD_RATE, DATA_BITS, STOP_BITS);
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

    private String listAvailablePorts() {
        SerialPort[] ports = SerialPort.getCommPorts();
        if (ports.length == 0) {
            return "none";
        }

        StringBuilder sb = new StringBuilder();
        for (SerialPort port : ports) {
            if (sb.length() > 0)
                sb.append(", ");
            sb.append(port.getSystemPortName()).append(" (").append(port.getDescriptivePortName()).append(")");
        }
        return sb.toString();
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
            return PREFERRED_BAUD_RATE;
        }
    }
}

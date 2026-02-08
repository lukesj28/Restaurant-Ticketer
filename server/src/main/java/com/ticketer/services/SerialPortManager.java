package com.ticketer.services;

import com.fazecast.jSerialComm.SerialPort;
import com.ticketer.exceptions.PrinterException;

import jakarta.annotation.PreDestroy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.OutputStream;

/**
 * Singleton manager for the serial port connection to the receipt printer.
 * Keeps the SerialPort object alive as a long-lived variable to avoid
 * repeatedly opening/closing the port which can cause communication issues.
 */
@Component
public class SerialPortManager {

    private static final Logger logger = LoggerFactory.getLogger(SerialPortManager.class);

    // Preferred baud rate, with fallback to highest available
    private static final int PREFERRED_BAUD_RATE = 38400;
    private static final int[] FALLBACK_BAUD_RATES = { 19200, 9600, 4800, 2400, 1200 };

    // ESC/POS Initialize command to clear printer buffer
    private static final byte[] ESC_POS_INIT = { 0x1B, 0x40 };

    // Port configuration
    private static final int DATA_BITS = 8;
    private static final int STOP_BITS = SerialPort.ONE_STOP_BIT;
    private static final int PARITY = SerialPort.NO_PARITY;
    private static final int TIMEOUT_MS = 5000;

    private final String printerPortName;
    private SerialPort serialPort;
    private final Object portLock = new Object();

    public SerialPortManager() {
        // Default printer port - can be configured via settings later
        this.printerPortName = "TM-m30II_020940";
    }

    /**
     * Get the output stream for the printer, initializing the connection if needed.
     * Sends ESC/POS initialize command at the start of each printing session.
     * 
     * @return OutputStream for writing ESC/POS commands
     * @throws PrinterException if the printer cannot be connected
     */
    public OutputStream getOutputStream() {
        synchronized (portLock) {
            ensurePortOpen();
            sendInitCommand();
            return serialPort.getOutputStream();
        }
    }

    /**
     * Ensure the serial port is open and ready for communication.
     */
    private void ensurePortOpen() {
        if (serialPort != null && serialPort.isOpen()) {
            logger.debug("Serial port already open");
            return;
        }

        serialPort = findPrinterPort();
        if (serialPort == null) {
            throw new PrinterException("Printer not found. Available ports: " + listAvailablePorts());
        }

        configurePort(serialPort);

        if (!serialPort.openPort()) {
            throw new PrinterException("Failed to open printer port: " + serialPort.getSystemPortName());
        }

        logger.info("Opened serial port {} at {} baud",
                serialPort.getSystemPortName(), serialPort.getBaudRate());
    }

    /**
     * Find the printer serial port by name pattern.
     */
    private SerialPort findPrinterPort() {
        SerialPort[] ports = SerialPort.getCommPorts();

        for (SerialPort port : ports) {
            String portName = port.getSystemPortName();
            String description = port.getDescriptivePortName();

            logger.debug("Found port: {} ({})", portName, description);

            // Match by port name or description containing the printer identifier
            if (portName.contains(printerPortName) || description.contains(printerPortName)) {
                logger.info("Found printer port: {} ({})", portName, description);
                return port;
            }
        }

        // Try matching by "tty" prefix for macOS serial devices
        for (SerialPort port : ports) {
            String portName = port.getSystemPortName();
            if (portName.startsWith("tty.") && portName.contains("TM")) {
                logger.info("Found Epson TM printer port: {}", portName);
                return port;
            }
        }

        return null;
    }

    /**
     * Configure the serial port with optimal settings for ESC/POS printers.
     */
    private void configurePort(SerialPort port) {
        // Try preferred baud rate first
        port.setBaudRate(PREFERRED_BAUD_RATE);
        port.setNumDataBits(DATA_BITS);
        port.setNumStopBits(STOP_BITS);
        port.setParity(PARITY);

        // Use non-blocking write to avoid timeout issues
        // The printer will buffer data and we don't need to wait for acknowledgment
        port.setComPortTimeouts(
                SerialPort.TIMEOUT_NONBLOCKING,
                0,
                0);

        logger.info("Configured port with baud rate: {}, data bits: {}, stop bits: {}, parity: none",
                PREFERRED_BAUD_RATE, DATA_BITS, STOP_BITS);
    }

    /**
     * Send the ESC/POS initialize command to clear the printer's buffer.
     * This should be called at the start of every printing session.
     */
    private void sendInitCommand() {
        if (serialPort == null || !serialPort.isOpen()) {
            throw new PrinterException("Serial port is not open");
        }

        try {
            // Small delay to let printer settle after port open
            Thread.sleep(100);

            // Write directly using jSerialComm's write method which is more reliable
            int bytesWritten = serialPort.writeBytes(ESC_POS_INIT, ESC_POS_INIT.length);

            if (bytesWritten == ESC_POS_INIT.length) {
                logger.debug("Sent ESC/POS initialize command ({} bytes)", bytesWritten);
            } else {
                logger.warn("ESC/POS init command: wrote {} of {} bytes", bytesWritten, ESC_POS_INIT.length);
            }

            // Small delay to let printer process init command
            Thread.sleep(50);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.warn("Interrupted while sending init command");
        } catch (Exception e) {
            // Log but don't fail - the printer may still work without explicit init
            logger.warn("Failed to send ESC/POS init command: {} - continuing anyway", e.getMessage());
        }
    }

    /**
     * List all available serial ports for debugging.
     */
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

    /**
     * Close the serial port. Called on application shutdown.
     */
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

    /**
     * Check if the printer is currently connected and available.
     */
    public boolean isPrinterAvailable() {
        synchronized (portLock) {
            if (serialPort != null && serialPort.isOpen()) {
                return true;
            }
            // Check if we can find the printer port
            return findPrinterPort() != null;
        }
    }

    /**
     * Get the current baud rate of the connection.
     */
    public int getBaudRate() {
        synchronized (portLock) {
            if (serialPort != null) {
                return serialPort.getBaudRate();
            }
            return PREFERRED_BAUD_RATE;
        }
    }
}

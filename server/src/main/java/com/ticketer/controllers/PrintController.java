package com.ticketer.controllers;

import com.ticketer.api.ApiResponse;
import com.ticketer.services.PrintService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/print")
public class PrintController {

    private static final Logger logger = LoggerFactory.getLogger(PrintController.class);

    private final PrintService printService;

    @Autowired
    public PrintController(PrintService printService) {
        this.printService = printService;
    }

    @PostMapping("/ticket/{ticketId}")
    public ApiResponse<String> printTicket(@PathVariable("ticketId") int ticketId) {
        logger.info("Received request to print ticket {}", ticketId);
        printService.printTicket(ticketId);
        return ApiResponse.success("Receipt printed successfully");
    }

    @PostMapping("/ticket/{ticketId}/order/{orderIndex}")
    public ApiResponse<String> printOrder(
            @PathVariable("ticketId") int ticketId,
            @PathVariable("orderIndex") int orderIndex) {
        logger.info("Received request to print order {} for ticket {}", orderIndex, ticketId);
        printService.printOrder(ticketId, orderIndex);
        return ApiResponse.success("Order receipt printed successfully");
    }
}

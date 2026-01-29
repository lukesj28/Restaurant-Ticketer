package com.ticketer.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import com.ticketer.services.RestaurantStateService;

@RestController
@RequestMapping("/api")
public class SystemController {

    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(SystemController.class);

    private final RestaurantStateService restaurantStateService;

    @Autowired
    public SystemController(RestaurantStateService restaurantStateService) {
        this.restaurantStateService = restaurantStateService;
    }

    @GetMapping("/status")
    public com.ticketer.api.ApiResponse<Boolean> isOpen() {
        return com.ticketer.api.ApiResponse.success(restaurantStateService.isOpen());
    }

    @PostMapping("/open")
    public com.ticketer.api.ApiResponse<Void> open() {
        logger.info("Received request to force open restaurant");
        restaurantStateService.forceOpen();
        return com.ticketer.api.ApiResponse.success(null);
    }

    @PostMapping("/shutdown")
    public com.ticketer.api.ApiResponse<Void> shutdown() {
        logger.info("Received request to force shutdown restaurant");
        restaurantStateService.forceClose();
        return com.ticketer.api.ApiResponse.success(null);
    }
}

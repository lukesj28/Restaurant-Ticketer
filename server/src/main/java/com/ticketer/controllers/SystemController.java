package com.ticketer.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import com.ticketer.services.RestaurantStateService;

@RestController
@RequestMapping("/api")
public class SystemController {

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
        restaurantStateService.forceOpen();
        return com.ticketer.api.ApiResponse.success(null);
    }

    @PostMapping("/shutdown")
    public com.ticketer.api.ApiResponse<Void> shutdown() {
        restaurantStateService.forceClose();
        return com.ticketer.api.ApiResponse.success(null);
    }
}

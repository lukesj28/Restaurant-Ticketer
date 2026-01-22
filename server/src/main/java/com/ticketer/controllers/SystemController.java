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
    public boolean isOpen() {
        return restaurantStateService.isOpen();
    }

    @PostMapping("/shutdown")
    public void shutdown() {
        restaurantStateService.shutdown();
    }
}

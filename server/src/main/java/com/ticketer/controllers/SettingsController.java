package com.ticketer.controllers;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import com.ticketer.api.ApiResponse;
import com.ticketer.dtos.*;
import com.ticketer.models.Settings;
import com.ticketer.services.RestaurantStateService;
import com.ticketer.services.SettingsService;

@RestController
@RequestMapping("/api/settings")
public class SettingsController {

    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(SettingsController.class);

    private final SettingsService settingsService;
    private final RestaurantStateService restaurantStateService;
    private final com.ticketer.components.SerialPortManager serialPortManager;

    @Autowired
    public SettingsController(SettingsService settingsService, RestaurantStateService restaurantStateService,
            com.ticketer.components.SerialPortManager serialPortManager) {
        this.settingsService = settingsService;
        this.restaurantStateService = restaurantStateService;
        this.serialPortManager = serialPortManager;
    }

    @GetMapping("/refresh")
    public ApiResponse<Settings> refreshSettings() {
        return ApiResponse.success(settingsService.getSettings());
    }

    @GetMapping("/tax")
    public ApiResponse<Integer> getTax() {
        return ApiResponse.success(settingsService.getTax());
    }

    @GetMapping("/hours")
    public ApiResponse<Map<String, String>> getOpeningHours() {
        return ApiResponse.success(settingsService.getAllOpeningHours());
    }

    @GetMapping("/hours/{day}")
    public ApiResponse<String> getOpeningHours(@PathVariable String day) {
        return ApiResponse.success(settingsService.getOpeningHours(day));
    }

    @GetMapping("/hours/{day}/open")
    public ApiResponse<String> getOpenTime(@PathVariable String day) {
        return ApiResponse.success(settingsService.getOpenTime(day));
    }

    @GetMapping("/hours/{day}/close")
    public ApiResponse<String> getCloseTime(@PathVariable("day") String day) {
        return ApiResponse.success(settingsService.getCloseTime(day));
    }

    @PutMapping("/tax")
    public ApiResponse<SettingsDto> setTax(@RequestBody Requests.TaxUpdateRequest request) {
        logger.info("Received request to update tax to: {}", request.tax());
        settingsService.setTax(request.tax());
        return ApiResponse.success(DtoMapper.toSettingsDto(settingsService.getSettings()));
    }

    @PutMapping("/hours/{day}")
    public ApiResponse<SettingsDto> setOpeningHours(@PathVariable("day") String day,
            @RequestBody Requests.OpeningHoursUpdateRequest request) {
        logger.info("Received request to update opening hours for {}: {}", day, request.hours());
        settingsService.setOpeningHours(day, request.hours());
        restaurantStateService.checkAndScheduleState();
        return ApiResponse.success(DtoMapper.toSettingsDto(settingsService.getSettings()));
    }

    @GetMapping("/printer/ports")
    public ApiResponse<java.util.List<String>> getAvailablePorts() {
        return ApiResponse.success(serialPortManager.getAvailablePorts());
    }

    @PutMapping("/printer")
    public ApiResponse<SettingsDto> updatePrinterSettings(@RequestBody Settings.PrinterSettings printerSettings) {
        logger.info("Received request to update printer settings");
        settingsService.setPrinterSettings(printerSettings);
        return ApiResponse.success(DtoMapper.toSettingsDto(settingsService.getSettings()));
    }

    @GetMapping("/restaurant")
    public ApiResponse<Settings.RestaurantDetails> getRestaurantDetails() {
        return ApiResponse.success(settingsService.getRestaurantDetails());
    }

    @PutMapping("/restaurant")
    public ApiResponse<SettingsDto> updateRestaurantDetails(@RequestBody Settings.RestaurantDetails restaurantDetails) {
        logger.info("Received request to update restaurant details");
        settingsService.setRestaurantDetails(restaurantDetails);
        return ApiResponse.success(DtoMapper.toSettingsDto(settingsService.getSettings()));
    }

    @GetMapping("/receipt")
    public ApiResponse<Settings.ReceiptSettings> getReceiptSettings() {
        return ApiResponse.success(settingsService.getReceiptSettings());
    }

    @PutMapping("/receipt")
    public ApiResponse<SettingsDto> updateReceiptSettings(@RequestBody Settings.ReceiptSettings receiptSettings) {
        logger.info("Received request to update receipt settings");
        settingsService.setReceiptSettings(receiptSettings);
        return ApiResponse.success(DtoMapper.toSettingsDto(settingsService.getSettings()));
    }
}

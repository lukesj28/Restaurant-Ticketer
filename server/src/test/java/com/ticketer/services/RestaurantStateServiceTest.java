package com.ticketer.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.Clock;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class RestaurantStateServiceTest {

    @Mock
    private SettingsService settingsService;

    @Mock
    private TicketService ticketService;

    private RestaurantStateService restaurantStateService;
    private Clock fixedClock;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        java.time.LocalDateTime localDate = java.time.LocalDateTime.parse("2023-01-02T12:00:00");
        fixedClock = Clock.fixed(localDate.atZone(ZoneId.systemDefault()).toInstant(), ZoneId.systemDefault());
    }

    private void initService() {
        restaurantStateService = new RestaurantStateService(settingsService, ticketService, fixedClock);
    }

    @Test
    public void testClosedWhenNoHours() {
        when(settingsService.getOpenTime(anyString())).thenReturn(null);
        when(settingsService.getCloseTime(anyString())).thenReturn(null);

        initService();
        restaurantStateService.init();
        assertFalse(restaurantStateService.isOpen());
    }

    @Test
    public void testClosedBeforeOpenTime() {
        when(settingsService.getOpenTime(anyString())).thenReturn("09:00");
        when(settingsService.getCloseTime(anyString())).thenReturn("17:00");
        java.time.LocalDateTime localDate = java.time.LocalDateTime.parse("2023-01-02T08:00:00");
        fixedClock = Clock.fixed(localDate.atZone(ZoneId.systemDefault()).toInstant(), ZoneId.systemDefault());
        initService();

        restaurantStateService.checkAndScheduleState();
        assertFalse(restaurantStateService.isOpen());
    }

    @Test
    public void testOpenDuringHours() {
        when(settingsService.getOpenTime(anyString())).thenReturn("09:00");
        when(settingsService.getCloseTime(anyString())).thenReturn("17:00");
        java.time.LocalDateTime localDate = java.time.LocalDateTime.parse("2023-01-02T12:00:00");
        fixedClock = Clock.fixed(localDate.atZone(ZoneId.systemDefault()).toInstant(), ZoneId.systemDefault());
        initService();

        restaurantStateService.checkAndScheduleState();
        assertTrue(restaurantStateService.isOpen());
    }

    @Test
    public void testClosedAfterHours() {
        when(settingsService.getOpenTime(anyString())).thenReturn("09:00");
        when(settingsService.getCloseTime(anyString())).thenReturn("17:00");
        java.time.LocalDateTime localDate = java.time.LocalDateTime.parse("2023-01-02T18:00:00");
        fixedClock = Clock.fixed(localDate.atZone(ZoneId.systemDefault()).toInstant(), ZoneId.systemDefault());
        initService();

        restaurantStateService.checkAndScheduleState();
        assertFalse(restaurantStateService.isOpen());
    }

    @Test
    public void testTransitionsFromOpenToClosed() {
        when(settingsService.getOpenTime(anyString())).thenReturn("09:00");
        when(settingsService.getCloseTime(anyString())).thenReturn("17:00");
        java.time.LocalDateTime localDate = java.time.LocalDateTime.parse("2023-01-02T18:00:00");
        fixedClock = Clock.fixed(localDate.atZone(ZoneId.systemDefault()).toInstant(), ZoneId.systemDefault());
        initService();

        restaurantStateService.checkAndScheduleState();
        assertFalse(restaurantStateService.isOpen());
    }

    @Test
    public void testParseError() {
        when(settingsService.getOpenTime(anyString())).thenReturn("invalid");
        when(settingsService.getCloseTime(anyString())).thenReturn("invalid");
        initService();

        restaurantStateService.checkAndScheduleState();
        assertFalse(restaurantStateService.isOpen());
    }

    @Test
    public void testCleanup() {
        initService();
        restaurantStateService.cleanup();
    }

    @Test
    public void testForceClose() {
        when(settingsService.getOpenTime(anyString())).thenReturn("09:00");
        when(settingsService.getCloseTime(anyString())).thenReturn("17:00");
        java.time.LocalDateTime localDate = java.time.LocalDateTime.parse("2023-01-02T12:00:00");
        fixedClock = Clock.fixed(localDate.atZone(ZoneId.systemDefault()).toInstant(), ZoneId.systemDefault());
        initService();

        restaurantStateService.checkAndScheduleState();
        assertTrue(restaurantStateService.isOpen());

        restaurantStateService.forceClose();
        assertFalse(restaurantStateService.isOpen());

        restaurantStateService.checkAndScheduleState();
        assertFalse(restaurantStateService.isOpen());
    }

    @Test
    public void testForceOpen() {
        when(settingsService.getOpenTime(anyString())).thenReturn("09:00");
        when(settingsService.getCloseTime(anyString())).thenReturn("17:00");
        java.time.LocalDateTime localDate = java.time.LocalDateTime.parse("2023-01-02T19:00:00");
        fixedClock = Clock.fixed(localDate.atZone(ZoneId.systemDefault()).toInstant(), ZoneId.systemDefault());
        initService();

        restaurantStateService.checkAndScheduleState();
        assertFalse(restaurantStateService.isOpen());

        restaurantStateService.forceOpen();
        assertTrue(restaurantStateService.isOpen());

        restaurantStateService.checkAndScheduleState();
        assertTrue(restaurantStateService.isOpen());
    }

    @Test
    public void testClosingSequenceInterruptionCleanUp() throws InterruptedException {
        initService();
        when(ticketService.areAllTicketsClosed()).thenReturn(false);

        Thread thread = new Thread(() -> {
            try {
                java.lang.reflect.Method method = RestaurantStateService.class.getDeclaredMethod("runClosingSequence");
                method.setAccessible(true);
                method.invoke(restaurantStateService);
            } catch (Exception e) {
            }
        });

        thread.start();
        Thread.sleep(100);

        thread.interrupt();
        thread.join(1000);

        verify(ticketService, atLeastOnce()).discardActiveTickets();
        verify(ticketService, atLeastOnce()).forceCloseCompletedTickets();
        verify(ticketService, atLeastOnce()).serializeClosedTickets();
        verify(ticketService, atLeastOnce()).deleteRecoveryFile();
    }

    @Test
    public void testClosingSequenceCompletesWhenAllClosed() throws Exception {
        initService();
        when(ticketService.areAllTicketsClosed()).thenReturn(true);

        java.lang.reflect.Method method = RestaurantStateService.class.getDeclaredMethod("runClosingSequence");
        method.setAccessible(true);
        method.invoke(restaurantStateService);

        verify(ticketService).discardActiveTickets();
        verify(ticketService).forceCloseCompletedTickets();
        verify(ticketService).serializeClosedTickets();
        verify(ticketService).deleteRecoveryFile();
    }
}

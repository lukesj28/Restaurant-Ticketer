package com.ticketer;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(Suite.class)
@Suite.SuiteClasses({
        com.ticketer.models.OrderItemTest.class,
        com.ticketer.utils.menu.dto.MenuItemTest.class,
        com.ticketer.utils.menu.dto.MenuItemViewTest.class,
        com.ticketer.utils.menu.dto.SideTest.class,
        com.ticketer.models.SettingsTest.class,
        com.ticketer.models.OrderTest.class,
        com.ticketer.models.TicketTest.class,
        com.ticketer.utils.ticket.TicketUtilsTest.class,
        com.ticketer.services.SettingsServiceTest.class,
        com.ticketer.services.MenuServiceTest.class,
        com.ticketer.services.TicketServiceTest.class,
        com.ticketer.controllers.MainControllerTest.class,
        com.ticketer.repositories.FileMenuRepositoryTest.class,
        com.ticketer.repositories.FileSettingsRepositoryTest.class
})
public class ServerTestSuite {

}

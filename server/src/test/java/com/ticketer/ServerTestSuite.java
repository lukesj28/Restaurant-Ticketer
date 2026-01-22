package com.ticketer;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(Suite.class)
@Suite.SuiteClasses({
        com.ticketer.models.OrderItemTest.class,
        com.ticketer.models.MenuItemTest.class,
        com.ticketer.models.MenuItemViewTest.class,
        com.ticketer.models.SideTest.class,
        com.ticketer.models.SettingsTest.class,
        com.ticketer.models.OrderTest.class,
        com.ticketer.models.TicketTest.class,
        com.ticketer.utils.ticket.TicketUtilsTest.class,
        com.ticketer.services.SettingsServiceTest.class,
        com.ticketer.services.MenuServiceTest.class,
        com.ticketer.services.TicketServiceTest.class,
        com.ticketer.controllers.SystemControllerTest.class,
        com.ticketer.controllers.MenuControllerTest.class,
        com.ticketer.controllers.SettingsControllerTest.class,
        com.ticketer.controllers.TicketControllerTest.class,
        com.ticketer.repositories.FileMenuRepositoryTest.class,
        com.ticketer.repositories.FileSettingsRepositoryTest.class
})
public class ServerTestSuite {

}

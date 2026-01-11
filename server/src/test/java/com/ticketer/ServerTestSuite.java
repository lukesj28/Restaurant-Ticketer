package com.ticketer;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(Suite.class)
@Suite.SuiteClasses({
                com.ticketer.utils.menu.MenuReaderTest.class,
                com.ticketer.utils.menu.MenuEditorTest.class,
                com.ticketer.models.ItemTest.class,
                com.ticketer.utils.menu.dto.ComplexItemTest.class,
                com.ticketer.utils.menu.dto.MenuItemViewTest.class,
                com.ticketer.utils.menu.dto.SideTest.class,
                com.ticketer.utils.settings.SettingsReaderTest.class,
                com.ticketer.utils.settings.SettingsEditorTest.class
})
public class ServerTestSuite {

}

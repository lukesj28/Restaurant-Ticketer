import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(Suite.class)
@Suite.SuiteClasses({
        utils.menu.MenuReaderTest.class,
        utils.menu.MenuEditorTest.class,
        models.ItemTest.class,
        utils.menu.dto.ComplexItemTest.class,
        utils.menu.dto.MenuItemViewTest.class,
        utils.menu.dto.SideTest.class
})
public class ServerTestSuite {
    // This class remains empty, used only as a holder for the above annotations
}

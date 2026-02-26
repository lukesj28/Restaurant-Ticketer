package com.ticketer.services;

import com.ticketer.models.BaseItem;
import com.ticketer.models.CategoryEntry;
import com.ticketer.models.Menu;
import com.ticketer.models.MenuItem;
import com.ticketer.repositories.MenuRepository;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.*;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

public class MenuServiceConcurrencyTest {

    @Test
    public void testConcurrentAccess() throws InterruptedException, ExecutionException {
        MenuRepository repo = mock(MenuRepository.class);

        UUID initialId = UUID.randomUUID();
        BaseItem initialItem = new BaseItem(initialId, "Starter", 500, true, false);
        Map<UUID, BaseItem> baseItems = new LinkedHashMap<>();
        baseItems.put(initialId, initialItem);
        List<MenuItem> items = new ArrayList<>();
        items.add(new MenuItem(initialId, Collections.emptyList()));
        Map<String, CategoryEntry> categories = new LinkedHashMap<>();
        categories.put("test", new CategoryEntry(true, items));
        Menu menu = new Menu(baseItems, categories, new LinkedHashMap<>(), new ArrayList<>(categories.keySet()));
        when(repo.getMenu()).thenReturn(menu);

        MenuService service = new MenuService(repo);

        int threads = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        List<Callable<Void>> tasks = new ArrayList<>();

        for (int i = 0; i < 5; i++) {
            tasks.add(() -> {
                for (int j = 0; j < 500; j++) {
                    try {
                        service.getMenu();
                        service.getCategoryOrder();
                    } catch (Exception e) {
                        if (e instanceof java.util.ConcurrentModificationException) {
                            throw new RuntimeException("CME detected", e);
                        }
                    }
                }
                return null;
            });
        }

        for (int i = 0; i < 5; i++) {
            tasks.add(() -> {
                for (int j = 0; j < 50; j++) {
                    try {
                        BaseItem created = service.createBaseItem("Item-" + UUID.randomUUID(), 100, false);
                        service.updateBaseItemPrice(created.getId(), 200);
                        service.deleteBaseItem(created.getId());
                    } catch (Exception e) {
                        throw new RuntimeException("Writer failed: " + e.getMessage(), e);
                    }
                }
                return null;
            });
        }

        List<Future<Void>> futures = executor.invokeAll(tasks);
        executor.shutdown();
        boolean terminated = executor.awaitTermination(15, TimeUnit.SECONDS);
        assertTrue(terminated, "Executor did not terminate in time");

        for (Future<Void> f : futures) {
            f.get();
        }
    }
}

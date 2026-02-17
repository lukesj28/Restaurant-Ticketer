package com.ticketer.services;

import com.ticketer.models.Menu;
import com.ticketer.models.MenuItem;
import com.ticketer.repositories.MenuRepository;
import org.junit.jupiter.api.Test;
import java.util.*;
import java.util.concurrent.*;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

public class MenuServiceConcurrencyTest {

    @Test
    public void testConcurrentAccess() throws InterruptedException, ExecutionException {
        MenuRepository repo = mock(MenuRepository.class);
        Map<String, List<MenuItem>> data = new HashMap<>();
        data.put("test", new ArrayList<>());
        Menu menu = new Menu(data, new ArrayList<>());
        when(repo.getMenu()).thenReturn(menu);

        MenuService service = new MenuService(repo);

        int threads = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        List<Callable<Void>> tasks = new ArrayList<>();

        for (int i = 0; i < 5; i++) {
            tasks.add(() -> {
                for (int j = 0; j < 1000; j++) {
                    try {
                        service.getAllItems();
                        service.getCategories();
                        try {
                            service.getCategory("test");
                        } catch (Exception e) {
                        }
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
            final int id = i;
            tasks.add(() -> {
                for (int j = 0; j < 100; j++) {
                    String name = "Item-" + id + "-" + j;
                    try {
                        service.addItem("test", name, 100, null);
                        service.editItemPrice("test", name, 200);
                        service.removeItem("test", name);
                    } catch (Exception e) {
                        throw new RuntimeException("Writer failed", e);
                    }
                }
                return null;
            });
        }

        List<Future<Void>> futures = executor.invokeAll(tasks);
        executor.shutdown();
        boolean terminated = executor.awaitTermination(10, TimeUnit.SECONDS);
        assertTrue(terminated, "Executor did not terminate in time");

        for (Future<Void> f : futures) {
            f.get();
        }
    }
}

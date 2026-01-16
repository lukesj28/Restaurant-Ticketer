package com.ticketer.repositories;

import com.ticketer.models.Menu;

public interface MenuRepository {
    Menu getMenu();

    void saveMenu(Menu menu);
}

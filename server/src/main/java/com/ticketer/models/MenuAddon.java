package com.ticketer.models;

public abstract class MenuAddon {
    public long price;
    public boolean available;
    public boolean kitchen;

    public MenuAddon() {}

    public MenuAddon(MenuAddon other) {
        this.price = other.price;
        this.available = other.available;
        this.kitchen = other.kitchen;
    }
}

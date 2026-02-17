package com.ticketer.models;

public class Side {
    public long price;
    public boolean available;
    public boolean kitchen;

    public Side() {}

    public Side(Side other) {
        this.price = other.price;
        this.available = other.available;
        this.kitchen = other.kitchen;
    }
}

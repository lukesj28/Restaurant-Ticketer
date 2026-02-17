package com.ticketer.models;

public class Extra {
    public long price;
    public boolean available;
    public boolean kitchen;

    public Extra() {}

    public Extra(Extra other) {
        this.price = other.price;
        this.available = other.available;
        this.kitchen = other.kitchen;
    }
}

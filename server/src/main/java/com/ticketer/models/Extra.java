package com.ticketer.models;

public class Extra {
    public long price;
    public boolean available;

    public Extra() {}

    public Extra(Extra other) {
        this.price = other.price;
        this.available = other.available;
    }
}

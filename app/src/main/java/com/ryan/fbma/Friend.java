package com.ryan.fbma;

import java.io.Serializable;

public class Friend implements Serializable {

    private final long id;
    private final String name;

    public Friend(final long id, final String name) {
        this.id = id;
        this.name = name;
    }

    public String getName() {
        return this.name;
    }

    private long getId() {
        return this.id;
    }

    @Override
    public boolean equals(Object other) {
        final Friend otherFriend = (Friend) other;

        return otherFriend.getId() == this.id;
    }

    @Override
    public String toString() {
        return "Name: " + this.name + " \tID: " + this.id;
    }
}
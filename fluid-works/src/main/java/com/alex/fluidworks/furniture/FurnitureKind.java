package com.alex.fluidworks.furniture;

public enum FurnitureKind {
    CHAIR("chair"),
    FOUR_LEGGED_TABLE("four_legged_table"),
    ONE_LEGGED_TABLE("one_legged_table");

    private final String id;

    FurnitureKind(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }
}

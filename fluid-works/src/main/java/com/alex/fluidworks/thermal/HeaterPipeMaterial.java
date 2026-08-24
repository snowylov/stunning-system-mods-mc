package com.alex.fluidworks.thermal;

public enum HeaterPipeMaterial {
    COPPER("copper", 24),
    IRON("iron", 12);

    private final String id;
    private final int conductionStep;

    HeaterPipeMaterial(String id, int conductionStep) {
        this.id = id;
        this.conductionStep = conductionStep;
    }

    public String id() { return id; }
    public int conductionStep() { return conductionStep; }
}

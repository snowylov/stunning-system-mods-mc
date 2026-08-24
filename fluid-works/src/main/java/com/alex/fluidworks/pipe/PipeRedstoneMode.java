package com.alex.fluidworks.pipe;

import net.minecraft.util.StringIdentifiable;

public enum PipeRedstoneMode implements StringIdentifiable {
    ALWAYS("always"),
    SIGNAL_HIGH("signal_high"),
    SIGNAL_LOW("signal_low");

    private final String id;

    PipeRedstoneMode(String id) {
        this.id = id;
    }

    public PipeRedstoneMode next() {
        PipeRedstoneMode[] values = values();
        return values[(ordinal() + 1) % values.length];
    }

    public boolean permits(boolean powered) {
        return switch (this) {
            case ALWAYS -> true;
            case SIGNAL_HIGH -> powered;
            case SIGNAL_LOW -> !powered;
        };
    }

    @Override
    public String asString() {
        return id;
    }
}

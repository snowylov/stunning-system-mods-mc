package com.alex.fluidworks.thermal;

/** Shared thermal contract for pipe blocks that carry a fluid temperature. */
public interface ThermalFluidCarrier {
    int fluidTemperature();
    void receiveTemperature(int temperature, long movedAmount);
    void approachTemperature(int target, int maximumStep);
}

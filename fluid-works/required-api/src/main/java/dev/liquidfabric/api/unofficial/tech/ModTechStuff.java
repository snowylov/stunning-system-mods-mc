package dev.liquidfabric.api.unofficial.tech;

/**
 * Intentionally separated module boundary for optional machines, generators,
 * chargers, energy systems, and future automation.
 *
 * Do not place liquid containers, needles, needle gun, utility fluid core,
 * BuildCraft-style tanks, pipes, or drums here.
 */
public final class ModTechStuff {
    private ModTechStuff() {}

    public static void register() {
        // Empty by design for this pass: tech-stuff is the reserved isolated expansion module.
    }
}

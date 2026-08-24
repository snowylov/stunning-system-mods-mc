package dev.liquidfabric.api.unofficial.client;

/**
 * Backward-compatible delegator. The real tooltip system is now shift-gated in
 * UtilityShiftTooltips.
 */
public final class SourceTaggedFluidTooltip {
    private SourceTaggedFluidTooltip() {}

    public static void registerClient() {
        UtilityShiftTooltips.registerClient();
    }
}

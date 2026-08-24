package dev.liquidfabric.api.unofficial.liquid;

/**
 * Compact constructor for a tint-overlay bottle. The item's model must put the
 * untinted bottle on layer0 and the fluid mask on layer1.
 */
public final class EasyBottleItem extends BaseLiquidContainerItem {
    public EasyBottleItem(Settings settings, long capacityDroplets, boolean potionLiquidsAllowed,
                          boolean drinkable, int sips, boolean returnsGlassBottle) {
        super(settings, capacityDroplets, potionLiquidsAllowed, drinkable, sips,
                returnsGlassBottle, 8, 1.0, 0);
    }
}

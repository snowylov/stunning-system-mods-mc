package dev.liquidfabric.api.unofficial.command;

import dev.liquidfabric.api.unofficial.UtilityApiMod;
import dev.liquidfabric.api.unofficial.core.*;
import dev.liquidfabric.api.unofficial.liquid.ModLiquidContainers;
import dev.liquidfabric.api.unofficial.needle.NeedlePayload;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.command.CommandSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public final class UtilityApiCommands {
    private UtilityApiCommands() {}

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> dispatcher.register(
                literal("liquid-fabric-api-unofficial-fabric-api")
                        .requires(source -> source.hasPermissionLevel(2))
                        .then(literal("give_liquid_container")
                                .then(argument("player", net.minecraft.command.argument.EntityArgumentType.player())
                                        .then(argument("container", StringArgumentType.word())
                                                .suggests((ctx, builder) -> CommandSource.suggestMatching(containerIds(), builder))
                                                .then(argument("liquid", StringArgumentType.string())
                                                        .suggests((ctx, builder) -> CommandSource.suggestMatching(liquidIds(), builder))
                                                        .executes(ctx -> execute(ctx.getSource(),
                                                                net.minecraft.command.argument.EntityArgumentType.getPlayer(ctx, "player"),
                                                                StringArgumentType.getString(ctx, "container"),
                                                                StringArgumentType.getString(ctx, "liquid"),
                                                                -1,
                                                                ""))
                                                        .then(argument("amount_mb", IntegerArgumentType.integer(0))
                                                                .executes(ctx -> execute(ctx.getSource(),
                                                                        net.minecraft.command.argument.EntityArgumentType.getPlayer(ctx, "player"),
                                                                        StringArgumentType.getString(ctx, "container"),
                                                                        StringArgumentType.getString(ctx, "liquid"),
                                                                        IntegerArgumentType.getInteger(ctx, "amount_mb"),
                                                                        ""))
                                                                .then(argument("source_flags", StringArgumentType.greedyString())
                                                                        .suggests((ctx, builder) -> CommandSource.suggestMatching(List.of("not_from_ocean", "from_cave", "not_from_ocean,from_cave"), builder))
                                                                        .executes(ctx -> execute(ctx.getSource(),
                                                                                net.minecraft.command.argument.EntityArgumentType.getPlayer(ctx, "player"),
                                                                                StringArgumentType.getString(ctx, "container"),
                                                                                StringArgumentType.getString(ctx, "liquid"),
                                                                                IntegerArgumentType.getInteger(ctx, "amount_mb"),
                                                                                StringArgumentType.getString(ctx, "source_flags")))))))))));
    }

    private static int execute(ServerCommandSource source, ServerPlayerEntity player, String containerArg, String liquidArg, int amountMb, String flags) {
        Item item = ModLiquidContainers.CONTAINERS_BY_NAME.get(containerArg);
        if (item == null) {
            source.sendError(Text.literal("Unknown liquid container: " + containerArg));
            return 0;
        }
        Identifier liquidId = parseId(liquidArg);
        if (LiquidRegistry.get(liquidId).isEmpty() && Registries.FLUID.get(liquidId) == net.minecraft.fluid.Fluids.EMPTY) {
            source.sendError(Text.literal("Unknown fluid/liquid: " + liquidId));
            return 0;
        }

        long capacity = FluidOverlayItem.getCapacity(item);
        long amount = amountMb < 0 ? capacity : FluidUnits.mbToDroplets(amountMb);
        amount = Math.min(capacity, amount);

        SourceFluidAttributes attrs = parseFlags(flags);
        ItemStack stack = new ItemStack(item);
        stack.set(ModComponents.STORED_FLUID, new StoredFluidComponent(liquidId, amount, attrs));
        if (liquidId.equals(UtilityApiMod.id("potion"))) {
            stack.set(ModComponents.NEEDLE_PAYLOAD, new NeedlePayload(liquidId, amount, List.of(), 0, 0, attrs));
        }
        player.getInventory().offerOrDrop(stack);
        source.sendFeedback(() -> Text.literal("Gave " + player.getName().getString() + " " + stack.getName().getString() + " containing " + FluidUnits.dropletsToMb(amount) + " mB of " + liquidId), true);
        return 1;
    }

    private static Identifier parseId(String value) {
        return value.contains(":") ? Identifier.of(value) : UtilityApiMod.id(value);
    }

    private static SourceFluidAttributes parseFlags(String flags) {
        String lower = flags == null ? "" : flags.toLowerCase(Locale.ROOT);
        return new SourceFluidAttributes(lower.contains("not_from_ocean"), lower.contains("from_cave"));
    }

    private static List<String> containerIds() {
        return new ArrayList<>(ModLiquidContainers.CONTAINERS_BY_NAME.keySet());
    }

    private static List<String> liquidIds() {
        List<String> values = new ArrayList<>();
        LiquidRegistry.values().forEach(type -> values.add(type.id().toString()));
        Registries.FLUID.getIds().forEach(id -> values.add(id.toString()));
        values.add("liquid-fabric-api-unofficial-fabric-api:potion");
        values.add("liquid-fabric-api-unofficial-fabric-api:milk");
        values.add("liquid-fabric-api-unofficial-fabric-api:chocolate_milk");
        values.add("liquid-fabric-api-unofficial-fabric-api:hot_chocolate");
        return values.stream().distinct().sorted().toList();
    }
}

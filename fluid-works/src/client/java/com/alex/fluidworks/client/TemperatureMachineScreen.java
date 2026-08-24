package com.alex.fluidworks.client;

import com.alex.fluidworks.thermal.TemperatureMachineScreenHandler;
import com.alex.fluidworks.thermal.ThermalApiBridge;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.Text;

/** Vanilla-widget slider; every change is validated again by the server handler. */
public final class TemperatureMachineScreen extends HandledScreen<TemperatureMachineScreenHandler> {
    public TemperatureMachineScreen(TemperatureMachineScreenHandler handler,
                                    PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
        backgroundWidth = 176;
        backgroundHeight = 166;
    }

    @Override protected void init() {
        super.init();
        double initial = handler.heating()
            ? (handler.targetTemperature() - ThermalApiBridge.STANDARD)
                / (double) (ThermalApiBridge.HOT - ThermalApiBridge.STANDARD)
            : (handler.targetTemperature() - ThermalApiBridge.COLD)
                / (double) (ThermalApiBridge.STANDARD - ThermalApiBridge.COLD);
        addDrawableChild(new TemperatureSlider(x + 12, y + 68, 152, 20,
            Math.max(0.0D, Math.min(1.0D, initial))));
    }

    @Override protected void drawBackground(DrawContext context, float deltaTicks, int mouseX, int mouseY) {
        int panel = handler.heating() ? 0xFF9A4D24 : 0xFF3F759A;
        context.fill(x, y, x + backgroundWidth, y + backgroundHeight, 0xFFC6C6C6);
        context.drawStrokedRectangle(x, y, backgroundWidth, backgroundHeight, 0xFF373737);
        context.fill(x + 7, y + 13, x + backgroundWidth - 7, y + 65, panel);
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 3; column++) {
                int sx = x + 61 + column * 18;
                int sy = y + 16 + row * 18;
                context.fill(sx, sy, sx + 18, sy + 18, 0xFF373737);
                context.fill(sx + 1, sy + 1, sx + 17, sy + 17, 0xFF8B8B8B);
            }
        }
    }

    private final class TemperatureSlider extends SliderWidget {
        private TemperatureSlider(int x, int y, int width, int height, double value) {
            super(x, y, width, height, Text.empty(), value);
            updateMessage();
        }

        @Override protected void updateMessage() {
            int shown = handler.heating()
                ? ThermalApiBridge.STANDARD + Math.round(
                    (ThermalApiBridge.HOT - ThermalApiBridge.STANDARD) * (float) value)
                : ThermalApiBridge.COLD + Math.round(
                    (ThermalApiBridge.STANDARD - ThermalApiBridge.COLD) * (float) value);
            setMessage(Text.translatable("gui.fluidworks.target_temperature", shown));
        }

        @Override protected void applyValue() {
            if (client != null && client.interactionManager != null) {
                client.interactionManager.clickButton(handler.syncId,
                    Math.max(0, Math.min(1000, (int) Math.round(value * 1000.0D))));
            }
        }
    }
}

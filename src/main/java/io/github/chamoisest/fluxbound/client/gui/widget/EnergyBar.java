package io.github.chamoisest.fluxbound.client.gui.widget;

import io.github.chamoisest.fluxbound.Fluxbound;
import io.github.chamoisest.fluxbound.machine.energy.EnergyFormat;
import io.github.chamoisest.fluxbound.network.menu.MachineOverviewPayload;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;

import java.util.List;

// Vertical FE bar for the energy column left of the content area, with a tooltip on hover.
public final class EnergyBar
{
    public static final int WIDTH = 18;
    public static final int HEIGHT = 76;

    private static final Identifier FRAME_SPRITE = Identifier.fromNamespaceAndPath(Fluxbound.MODID, "energy_bar");
    private static final Identifier FILL_SPRITE = Identifier.fromNamespaceAndPath(Fluxbound.MODID, "energy_bar_fill");
    private static final int FILL_WIDTH = WIDTH - 2;
    private static final int FILL_HEIGHT = HEIGHT - 2;

    private EnergyBar() {}

    // A null overview means the server hasn't sent data yet; the bar stays empty and the tooltip says so.
    public static void extract(GuiGraphicsExtractor graphics, Font font, int x, int y,
                               @Nullable MachineOverviewPayload overview, int mouseX, int mouseY)
    {
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, FRAME_SPRITE, x, y, WIDTH, HEIGHT);

        int filled = overview == null ? 0 : filledHeight(overview.energy(), overview.capacity());

        // Draws only the bottom part of the fill sprite, so the bar fills from the bottom up.
        if (filled > 0)
        {
            int top = FILL_HEIGHT - filled;
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, FILL_SPRITE, FILL_WIDTH, FILL_HEIGHT, 0, top,
                    x + 1, y + 1 + top, FILL_WIDTH, filled);
        }

        boolean hovered = mouseX >= x && mouseX < x + WIDTH && mouseY >= y && mouseY < y + HEIGHT;

        if (hovered)
        {
            graphics.setComponentTooltipForNextFrame(font, tooltip(overview), mouseX, mouseY);
        }
    }

    // Rounded down like the numbers, but any stored energy shows at least one pixel,
    // and only a completely full machine fills the bar.
    private static int filledHeight(long energy, long capacity)
    {
        if (energy <= 0 || capacity <= 0)
        {
            return 0;
        }

        if (energy >= capacity)
        {
            return FILL_HEIGHT;
        }

        int pixels = (int) ((double) energy / capacity * FILL_HEIGHT);
        return Math.clamp(pixels, 1, FILL_HEIGHT - 1);
    }

    private static List<Component> tooltip(@Nullable MachineOverviewPayload overview)
    {
        Component title = Component.translatable("gui.fluxbound.energy.title");

        if (overview == null)
        {
            return List.of(title, Component.translatable("gui.fluxbound.loading").withStyle(ChatFormatting.GRAY));
        }

        return List.of(
                title,
                Component.translatable("tooltip.fluxbound.stored").withStyle(ChatFormatting.GRAY)
                        .append(Component.literal(EnergyFormat.stored(overview.energy(), overview.capacity()))
                                .withStyle(ChatFormatting.WHITE)),
                Component.translatable("tooltip.fluxbound.max_input").withStyle(ChatFormatting.GRAY)
                        .append(Component.literal(EnergyFormat.perTick(overview.inputPerTick()))
                                .withStyle(ChatFormatting.WHITE))
        );
    }
}

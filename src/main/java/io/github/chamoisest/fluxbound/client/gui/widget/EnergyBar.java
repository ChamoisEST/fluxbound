package io.github.chamoisest.fluxbound.client.gui.widget;

import io.github.chamoisest.fluxbound.Fluxbound;
import io.github.chamoisest.fluxbound.machine.energy.EnergyFormat;
import io.github.chamoisest.fluxbound.network.menu.MachineOverviewPayload;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;

import java.util.function.Supplier;

// Vertical FE bar for the energy column left of the content area, with its values as tooltip on hover or
// keyboard focus (M1 B9).
public final class EnergyBar extends InfoWidget
{
    public static final int WIDTH = 18;
    public static final int HEIGHT = 76;

    private static final Identifier FRAME_SPRITE = Identifier.fromNamespaceAndPath(Fluxbound.MODID, "energy_bar");
    private static final Identifier FILL_SPRITE = Identifier.fromNamespaceAndPath(Fluxbound.MODID, "energy_bar_fill");
    private static final int FILL_WIDTH = WIDTH - 2;
    private static final int FILL_HEIGHT = HEIGHT - 2;

    // Null until the server has sent data; the bar stays empty and the tooltip says so.
    private final Supplier<@Nullable MachineOverviewPayload> overview;

    public EnergyBar(int x, int y, Supplier<@Nullable MachineOverviewPayload> overview)
    {
        super(x, y, WIDTH, HEIGHT, Component.translatable("gui.fluxbound.energy.title"));
        this.overview = overview;
    }

    @Override
    protected void extractContent(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a)
    {
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, FRAME_SPRITE, getX(), getY(), WIDTH, HEIGHT);

        MachineOverviewPayload current = overview.get();
        int filled = current == null ? 0 : filledHeight(current.energy(), current.capacity());

        // Draws only the bottom part of the fill sprite, so the bar fills from the bottom up.
        if (filled > 0)
        {
            int top = FILL_HEIGHT - filled;
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, FILL_SPRITE, FILL_WIDTH, FILL_HEIGHT, 0, top,
                    getX() + 1, getY() + 1 + top, FILL_WIDTH, filled);
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

    // One component with line breaks, because vanilla's Tooltip takes a single message.
    @Override
    protected Component tooltipContent()
    {
        MachineOverviewPayload current = overview.get();
        Component title = Component.translatable("gui.fluxbound.energy.title");

        if (current == null)
        {
            return Component.empty().append(title).append("\n")
                    .append(Component.translatable("gui.fluxbound.loading").withStyle(ChatFormatting.GRAY));
        }

        return Component.empty().append(title).append("\n")
                .append(Component.translatable("tooltip.fluxbound.stored").withStyle(ChatFormatting.GRAY)
                        .append(Component.literal(EnergyFormat.stored(current.energy(), current.capacity()))
                                .withStyle(ChatFormatting.WHITE)))
                .append("\n")
                .append(Component.translatable("tooltip.fluxbound.max_input").withStyle(ChatFormatting.GRAY)
                        .append(Component.literal(EnergyFormat.perTick(current.inputPerTick()))
                                .withStyle(ChatFormatting.WHITE)));
    }
}

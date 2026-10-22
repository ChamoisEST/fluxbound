package io.github.chamoisest.fluxbound.client.gui.widget;

import io.github.chamoisest.fluxbound.Fluxbound;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

// "?" marker that explains something in a tooltip. Hover-only for now (P10 debt, review-findings.md).
public final class HelpMarker
{
    public static final int SIZE = 9;

    private static final Identifier SPRITE = Identifier.fromNamespaceAndPath(Fluxbound.MODID, "help");
    private static final int TOOLTIP_WIDTH = 170;

    private HelpMarker() {}

    public static void extract(GuiGraphicsExtractor graphics, Font font, int x, int y, Component help,
                               int mouseX, int mouseY)
    {
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, SPRITE, x, y, SIZE, SIZE);

        // The scissor check keeps the marker from reacting while it is scrolled out of view.
        boolean hovered = mouseX >= x && mouseX < x + SIZE && mouseY >= y && mouseY < y + SIZE;

        if (hovered && graphics.containsPointInScissor(mouseX, mouseY))
        {
            graphics.setTooltipForNextFrame(font, font.split(help, TOOLTIP_WIDTH), mouseX, mouseY);
        }
    }
}

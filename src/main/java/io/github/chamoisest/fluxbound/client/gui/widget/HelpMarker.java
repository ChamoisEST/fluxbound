package io.github.chamoisest.fluxbound.client.gui.widget;

import io.github.chamoisest.fluxbound.Fluxbound;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.util.function.Supplier;

// "?" marker that explains something in its tooltip, on hover or keyboard focus (M1 B9).
public final class HelpMarker extends InfoWidget
{
    public static final int SIZE = 9;

    private static final Identifier SPRITE = Identifier.fromNamespaceAndPath(Fluxbound.MODID, "help");

    private final Supplier<Component> help;

    public HelpMarker(int x, int y, Supplier<Component> help)
    {
        super(x, y, SIZE, SIZE, Component.translatable("gui.fluxbound.help"));
        this.help = help;
    }

    @Override
    protected Component tooltipContent()
    {
        return help.get();
    }

    @Override
    protected void extractContent(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a)
    {
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, SPRITE, getX(), getY(), SIZE, SIZE);
    }
}

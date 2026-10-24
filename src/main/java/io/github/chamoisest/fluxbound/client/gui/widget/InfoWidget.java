package io.github.chamoisest.fluxbound.client.gui.widget;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;

import java.util.Objects;

// Something that only explains: no click action, but Tab can focus it, and then it shows its tooltip as when
// hovered (vanilla WidgetTooltipHolder) and the narrator reads it (M1 B9, K2/K3).
public abstract class InfoWidget extends AbstractWidget
{
    private static final int FOCUS_OUTLINE = 0xFFFFFFFF;

    private @Nullable Component shownTooltip;

    protected InfoWidget(int x, int y, int width, int height, Component name)
    {
        super(x, y, width, height, name);
    }

    // The current explanation, or null for none. Asked every frame, so it may change while the screen is open.
    protected abstract @Nullable Component tooltipContent();

    protected abstract void extractContent(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a);

    @Override
    protected final void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a)
    {
        extractContent(graphics, mouseX, mouseY, a);

        if (isFocused())
        {
            // A hollow 1 px frame just outside, like the highlight of our fields; a filled one would show
            // through the transparent parts of the "?" sprite.
            graphics.fill(getX() - 1, getY() - 1, getRight() + 1, getY(), FOCUS_OUTLINE);
            graphics.fill(getX() - 1, getBottom(), getRight() + 1, getBottom() + 1, FOCUS_OUTLINE);
            graphics.fill(getX() - 1, getY(), getX(), getBottom(), FOCUS_OUTLINE);
            graphics.fill(getRight(), getY(), getRight() + 1, getBottom(), FOCUS_OUTLINE);
        }

        // Replaced only when the text changes, because vanilla's Tooltip caches its wrapped lines.
        Component content = tooltipContent();

        if (!Objects.equals(content, shownTooltip))
        {
            shownTooltip = content;
            setTooltip(content == null ? null : Tooltip.create(content));
        }
    }

    // Nothing to press, so clicks pass through and never move keyboard focus here.
    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick)
    {
        return false;
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output)
    {
        output.add(NarratedElementType.TITLE, getMessage());
    }
}

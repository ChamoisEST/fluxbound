package io.github.chamoisest.fluxbound.client.gui.tab;

import io.github.chamoisest.fluxbound.Fluxbound;
import io.github.chamoisest.fluxbound.client.gui.widget.HelpMarker;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractScrollArea;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.resources.Identifier;

// Scrollable area that shows the selected tab. Vanilla's AbstractScrollArea provides the scroll amount,
// scrollbar, dragging and mouse wheel; this class adds the inset frame, clipping and the tab content.
public class MachineContentPanel extends AbstractScrollArea
{
    private static final Identifier INSET_SPRITE = Identifier.fromNamespaceAndPath(Fluxbound.MODID, "panel_inset");
    private static final int PADDING = 4;
    private static final int SCROLLBAR_GAP = 2;
    private static final int HELP_GAP = 3;
    private static final int SCROLL_RATE = 10;

    private final Font font;
    private MachineTab tab;

    // x, y, width and height describe the inner area; the inset frame is drawn one pixel around it.
    public MachineContentPanel(Font font, int x, int y, int width, int height)
    {
        super(x, y, width, height, CommonComponents.EMPTY, defaultSettings(SCROLL_RATE));
        this.font = font;
    }

    public void setTab(MachineTab tab)
    {
        this.tab = tab;
        setScrollAmount(0.0);
    }

    // Text stops before the "?" column; the scrollbar's space is always reserved, so text never reflows when
    // the scrollbar appears.
    public int contentWidth()
    {
        return helpX() - HELP_GAP - (getX() + PADDING);
    }

    // The "?" marker sits in its own column next to the scrollbar and doesn't scroll with the content.
    private int helpX()
    {
        return scrollBarX() - SCROLLBAR_GAP - HelpMarker.SIZE;
    }

    @Override
    protected int contentHeight()
    {
        return tab == null ? 0 : tab.contentHeight(font, contentWidth()) + 2 * PADDING;
    }

    // Tab widgets come before the panel among the screen's children and get their clicks directly; the panel
    // itself only handles its scrollbar.
    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick)
    {
        return updateScrolling(event);
    }

    @Override
    protected void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a)
    {
        // Vanilla clamps the scroll amount only when it is set, so content that got shorter (for example a removed
        // trusted player) would leave it past the end and draw the scroller below the panel.
        refreshScrollAmount();

        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, INSET_SPRITE, getX() - 1, getY() - 1, width + 2, height + 2);

        if (tab != null)
        {
            graphics.enableScissor(getX(), getY(), getRight(), getBottom());
            tab.extract(graphics, font, getX() + PADDING, getY() + PADDING - (int) scrollAmount(),
                    contentWidth(), mouseX, mouseY);
            extractTabWidgets(graphics, mouseX, mouseY, a);
            graphics.disableScissor();

            HelpMarker.extract(graphics, font, helpX(), getY() + PADDING, tab.help(), mouseX, mouseY);
        }

        extractScrollbar(graphics, mouseX, mouseY);
    }

    // A widget is shown and usable only while its tab shows widgets and it is fully inside the visible area, so its
    // hidden part can't be clicked and keyboard focus skips widgets scrolled out of view. A ClippedWidget is shown
    // while any part is visible and handles its hidden part itself.
    private void extractTabWidgets(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a)
    {
        boolean shown = tab.widgetsShown();

        for (AbstractWidget widget : tab.widgets())
        {
            if (widget instanceof ClippedWidget clipped)
            {
                widget.visible = shown && widget.getHeight() > 0
                        && widget.getBottom() > getY() && widget.getY() < getBottom();
                clipped.setViewport(getY(), getBottom(), this::reveal);
            }
            else
            {
                widget.visible = shown && widget.getY() >= getY() && widget.getBottom() <= getBottom();
            }

            widget.extractRenderState(graphics, mouseX, mouseY, a);
        }
    }

    private void reveal(int top, int bottom)
    {
        if (top < getY())
        {
            setScrollAmount(scrollAmount() - (getY() - top));
        }
        else if (bottom > getBottom())
        {
            setScrollAmount(scrollAmount() + (bottom - getBottom()));
        }
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output)
    {
        if (tab != null)
        {
            output.add(NarratedElementType.TITLE, tab.type().label());
        }
    }
}

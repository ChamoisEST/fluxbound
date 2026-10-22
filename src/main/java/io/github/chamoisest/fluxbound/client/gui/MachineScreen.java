package io.github.chamoisest.fluxbound.client.gui;

import com.mojang.blaze3d.platform.InputConstants;
import io.github.chamoisest.fluxbound.Fluxbound;
import io.github.chamoisest.fluxbound.client.gui.tab.MachineContentPanel;
import io.github.chamoisest.fluxbound.client.gui.tab.MachineTab;
import io.github.chamoisest.fluxbound.client.gui.tab.MachineTabBar;
import io.github.chamoisest.fluxbound.menu.MachineMenuLayout;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

// Common look of machine management screens: shared size, frame, slot backgrounds, tab row above the
// frame and scrollable content area. Machine-specific screens add their tabs and own content on top.
// Also hosts at most one overlay (an open dropdown list or a confirmation) that takes all input while open.
public abstract class MachineScreen<M extends AbstractContainerMenu> extends AbstractContainerScreen<M>
        implements OverlayHost
{
    private static final Identifier FRAME_SPRITE = Identifier.fromNamespaceAndPath(Fluxbound.MODID, "frame");

    // Vanilla's slot sprite, so resource packs that restyle slots also apply here.
    private static final Identifier SLOT_SPRITE = Identifier.withDefaultNamespace("container/slot");

    // The tab row sits above the frame; the content area fills the frame right of the energy column
    // (M1 decisions D1, B3b-2).
    private static final int CONTENT_X = 34;
    private static final int CONTENT_Y = 17;
    private static final int CONTENT_WIDTH = 188;
    private static final int CONTENT_HEIGHT = 109;

    private static final int LABEL_COLOR = 0xFF404040;
    private static final int TITLE_STATUS_GAP = 6;
    private static final int STATUS_TOOLTIP_WIDTH = 170;

    private final List<MachineTab> tabs = new ArrayList<>();
    private int selectedTab;
    private MachineContentPanel contentPanel;
    private @Nullable ScreenOverlay overlay;
    private boolean ignoreNextRelease;

    protected MachineScreen(M menu, Inventory inventory, Component title)
    {
        super(menu, inventory, title, MachineMenuLayout.WIDTH, MachineMenuLayout.HEIGHT);
        inventoryLabelX = MachineMenuLayout.INVENTORY_X - 1;
    }

    // Called by subclasses in their constructor, in display order.
    protected void addTab(MachineTab tab)
    {
        tabs.add(tab);
    }

    // Optional state shown at the top right, opposite the title; null shows nothing.
    protected @Nullable MachineStatus status()
    {
        return null;
    }

    // Runs on opening and on every window resize; the selected tab survives because it is a field.
    @Override
    protected void init()
    {
        super.init();
        overlay = null;

        // Centre the frame together with the tab row above it, not the frame alone.
        topPos += MachineTabBar.HEIGHT / 2;

        if (tabs.isEmpty())
        {
            return;
        }

        // Tab widgets come first among the children, so clicks on them are not taken by the content panel.
        for (MachineTab tab : tabs)
        {
            for (AbstractWidget widget : tab.widgets())
            {
                addWidget(widget);
            }
        }

        contentPanel = addRenderableWidget(new MachineContentPanel(font,
                leftPos + CONTENT_X + 1, topPos + CONTENT_Y + 1, CONTENT_WIDTH - 2, CONTENT_HEIGHT - 2));
        contentPanel.setTab(tabs.get(selectedTab));

        addRenderableWidget(new MachineTabBar(font, leftPos, topPos - MachineTabBar.HEIGHT, imageWidth,
                tabs, selectedTab, this::selectTab));

        showTabWidgets();
    }

    // Clicks on the tab row above the frame must not count as "outside", or releasing the mouse there
    // would drop a carried item (vanilla's creative screen does the same for its tabs).
    @Override
    protected boolean hasClickedOutside(double mouseX, double mouseY, int left, int top)
    {
        boolean inTabRow = mouseX >= left && mouseX < left + imageWidth
                && mouseY >= top - MachineTabBar.HEIGHT && mouseY < top;

        return !inTabRow && super.hasClickedOutside(mouseX, mouseY, left, top);
    }

    private void selectTab(int index)
    {
        selectedTab = index;
        contentPanel.setTab(tabs.get(index));
        closeOverlay();
        showTabWidgets();
    }

    // Widgets of other tabs stay children but are hidden, so they neither take clicks nor keyboard focus.
    // The content panel decides the visibility of the selected tab's widgets each frame.
    private void showTabWidgets()
    {
        for (int index = 0; index < tabs.size(); index++)
        {
            for (AbstractWidget widget : tabs.get(index).widgets())
            {
                widget.visible = index == selectedTab;

                if (index != selectedTab && widget.isFocused())
                {
                    clearFocus();
                }
            }
        }
    }

    @Override
    public void openOverlay(ScreenOverlay overlay)
    {
        this.overlay = overlay;
    }

    @Override
    public void closeOverlay()
    {
        overlay = null;
    }

    @Override
    public boolean isOpen(ScreenOverlay overlay)
    {
        return this.overlay == overlay;
    }

    // Under an open overlay nothing else counts as hovered, so no highlight or tooltip shows through it. The
    // overlay is drawn last in its own stratum, above slots and items.
    @Override
    public void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a)
    {
        boolean covered = overlay != null && overlay.isMouseOver(mouseX, mouseY);

        super.extractContents(graphics, covered ? -1 : mouseX, covered ? -1 : mouseY, a);

        if (overlay != null)
        {
            graphics.nextStratum();
            overlay.extract(graphics, mouseX, mouseY);
        }
    }

    // While an overlay is open it is modal: a click inside goes to it, a click outside only closes it and never
    // reaches slots or widgets. The matching release is swallowed too.
    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick)
    {
        if (overlay == null)
        {
            return super.mouseClicked(event, doubleClick);
        }

        if (overlay.isMouseOver(event.x(), event.y()))
        {
            overlay.mouseClicked(event);
        }
        else
        {
            closeOverlay();
        }

        ignoreNextRelease = true;
        return true;
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event)
    {
        if (ignoreNextRelease)
        {
            ignoreNextRelease = false;
            return true;
        }

        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dx, double dy)
    {
        return overlay != null || ignoreNextRelease || super.mouseDragged(event, dx, dy);
    }

    // Esc and the inventory key close an open overlay first; only with no overlay open do they close the GUI
    // (user requirement, M1 B5-5, B5b-1). Vanilla closes the screen on Esc before children see the key, hence
    // this override. While an overlay is open no other key reaches the screen.
    @Override
    public boolean keyPressed(KeyEvent event)
    {
        if (overlay == null)
        {
            return super.keyPressed(event);
        }

        boolean closeKey = event.isEscape()
                || minecraft.options.keyInventory.isActiveAndMatches(InputConstants.getKey(event));

        if (!overlay.keyPressed(event) && closeKey)
        {
            closeOverlay();
        }

        return true;
    }

    // AbstractContainerScreen only handles bundle scrolling over slots and doesn't pass the wheel on to widgets.
    // The wheel goes to the first child under the mouse that uses it, so a widget inside the content panel
    // doesn't block scrolling. With an overlay open, the wheel only closes it.
    @Override
    public boolean mouseScrolled(double x, double y, double scrollX, double scrollY)
    {
        if (overlay != null)
        {
            closeOverlay();
            return true;
        }

        if (super.mouseScrolled(x, y, scrollX, scrollY))
        {
            return true;
        }

        for (GuiEventListener child : children())
        {
            if (child.isMouseOver(x, y) && child.mouseScrolled(x, y, scrollX, scrollY))
            {
                return true;
            }
        }

        return false;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a)
    {
        super.extractBackground(graphics, mouseX, mouseY, a);
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, FRAME_SPRITE, leftPos, topPos, imageWidth, imageHeight);

        for (Slot slot : menu.slots)
        {
            if (slot.isActive())
            {
                graphics.blitSprite(RenderPipelines.GUI_TEXTURED, SLOT_SPRITE,
                        leftPos + slot.x - 1, topPos + slot.y - 1, 18, 18);
            }
        }

        // Drawn here rather than in extractLabels, because the hover check needs absolute coordinates.
        MachineStatus status = status();

        if (status != null)
        {
            int statusX = leftPos + statusLeft(status);
            int statusY = topPos + titleLabelY;
            int statusWidth = font.width(status.text());

            graphics.text(font, status.text(), statusX, statusY, status.color(), false);

            if (mouseX >= statusX && mouseX < statusX + statusWidth
                    && mouseY >= statusY && mouseY < statusY + font.lineHeight)
            {
                graphics.setTooltipForNextFrame(font, font.split(status.help(), STATUS_TOOLTIP_WIDTH), mouseX, mouseY);
            }
        }
    }

    // Title on the left, shortened so it never runs into the status on the right.
    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY)
    {
        MachineStatus status = status();
        int titleRight = status == null ? imageWidth - titleLabelX : statusLeft(status) - TITLE_STATUS_GAP;

        graphics.text(font, ScreenText.fitToWidth(font, title, titleRight - titleLabelX), titleLabelX, titleLabelY,
                LABEL_COLOR, false);
        graphics.text(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, LABEL_COLOR, false);
    }

    private int statusLeft(MachineStatus status)
    {
        return imageWidth - titleLabelX - font.width(status.text());
    }
}

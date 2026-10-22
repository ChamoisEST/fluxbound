package io.github.chamoisest.fluxbound.client.gui.tab;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.network.chat.Component;

import java.util.List;

// One tab of a machine screen. Tabs draw their own content; MachineContentPanel clips and scrolls it.
public interface MachineTab
{
    MachineTabType type();

    // Explanation shown by the "?" marker at the top right of the content area.
    Component help();

    // Total content height at the given width, used for scrolling.
    int contentHeight(Font font, int width);

    // Draws the content with its top-left corner at x, y; y already includes the scroll offset.
    void extract(GuiGraphicsExtractor graphics, Font font, int x, int y, int width, int mouseX, int mouseY);

    // Widgets shown in the content area, created once by the tab. The screen makes them children (input and
    // keyboard focus), the tab positions them in extract, and the content panel draws them clipped (M1 D2).
    default List<? extends AbstractWidget> widgets()
    {
        return List.of();
    }

    // False while the tab shows something else instead of its widgets, such as a loading or no-access notice.
    default boolean widgetsShown()
    {
        return true;
    }
}

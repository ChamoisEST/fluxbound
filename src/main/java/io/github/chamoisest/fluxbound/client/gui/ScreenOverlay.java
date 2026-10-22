package io.github.chamoisest.fluxbound.client.gui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;

// Something drawn over the whole machine screen that takes all input while open, such as an open dropdown
// list or (B5c) a confirmation. MachineScreen hosts at most one at a time.
public interface ScreenOverlay
{
    void extract(GuiGraphicsExtractor graphics, int mouseX, int mouseY);

    boolean isMouseOver(double mouseX, double mouseY);

    // Only clicks inside the overlay arrive here; a click outside closes it.
    void mouseClicked(MouseButtonEvent event);

    // True if the key was used. Esc and the inventory key close the overlay when it doesn't use them.
    boolean keyPressed(KeyEvent event);
}

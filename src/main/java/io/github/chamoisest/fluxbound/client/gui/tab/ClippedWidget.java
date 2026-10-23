package io.github.chamoisest.fluxbound.client.gui.tab;

// A tab widget that may be taller than the content area (M1 B6b-1). The panel shows it while any part is
// visible and tells it the visible range every frame, so it can ignore clicks outside that range and keep its
// keyboard selection in view. Other tab widgets are shown only while fully visible.
public interface ClippedWidget
{
    // Screen coordinates of the visible part of the content area.
    void setViewport(int top, int bottom, ScrollTarget scroll);

    @FunctionalInterface
    interface ScrollTarget
    {
        // Scrolls the content area as little as needed to show the given screen rows.
        void reveal(int top, int bottom);
    }
}

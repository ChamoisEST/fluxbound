package io.github.chamoisest.fluxbound.client.gui;

// Lets widgets inside a machine screen open and close the screen's overlay.
public interface OverlayHost
{
    void openOverlay(ScreenOverlay overlay);

    void closeOverlay();

    boolean isOpen(ScreenOverlay overlay);
}

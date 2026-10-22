package io.github.chamoisest.fluxbound.menu;

// Shared size and player-inventory position of machine management GUIs (M1 decisions D1, B3b-2).
// Common code, because the menu creates the inventory slots on both sides.
public final class MachineMenuLayout
{
    // The frame; the tab row sits above it, so the whole screen is 230x240 (fits 320x240, P2).
    public static final int WIDTH = 230;
    public static final int HEIGHT = 222;

    // Item position of the first inventory slot; its 18x18 background starts one pixel up and left.
    // The 162-pixel grid is centred horizontally and uses vanilla's distance from the bottom edge.
    public static final int INVENTORY_X = 35;
    public static final int INVENTORY_Y = HEIGHT - 82;

    private MachineMenuLayout() {}
}

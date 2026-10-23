package io.github.chamoisest.fluxbound.client.gui.widget;

import net.minecraft.client.gui.GuiGraphicsExtractor;

// Fluxbound's own controls in the GUI's light grey, with 1 px bevels like vanilla panels (M1 B5-5, B6b-2):
// raised for things to press, sunken for things to type into. The vanilla button sprite (designed for 20 px)
// crowds the text at 14 px, and the vanilla text-field sprite is too dark against the content area.
public final class FieldStyle
{
    public static final int FILL = 0xFFC6C6C6;
    public static final int TEXT_COLOR = 0xFF404040;
    public static final int HINT_COLOR = 0xFF808080;
    public static final int DISABLED_TEXT_COLOR = 0xFF8B8B8B;

    private static final int BEVEL_LIGHT = 0xFFFFFFFF;
    private static final int BEVEL_DARK = 0xFF373737;
    // Drawn one pixel outside the control, over the mid-grey content area, when hovered, focused or open.
    private static final int HIGHLIGHT_OUTLINE = 0xFFFFFFFF;

    private FieldStyle() {}

    // White top and left edges, dark bottom and right edges.
    public static void extractRaised(GuiGraphicsExtractor graphics, int x, int y, int right, int bottom,
                                     boolean highlighted)
    {
        extractOutline(graphics, x, y, right, bottom, highlighted);
        graphics.fill(x, y, right, bottom, BEVEL_DARK);
        graphics.fill(x, y, right - 1, bottom - 1, BEVEL_LIGHT);
        graphics.fill(x + 1, y + 1, right - 1, bottom - 1, FILL);
    }

    // Dark top and left edges, white bottom and right edges.
    public static void extractSunken(GuiGraphicsExtractor graphics, int x, int y, int right, int bottom,
                                     boolean highlighted)
    {
        extractOutline(graphics, x, y, right, bottom, highlighted);
        graphics.fill(x, y, right, bottom, BEVEL_LIGHT);
        graphics.fill(x, y, right - 1, bottom - 1, BEVEL_DARK);
        graphics.fill(x + 1, y + 1, right - 1, bottom - 1, FILL);
    }

    private static void extractOutline(GuiGraphicsExtractor graphics, int x, int y, int right, int bottom,
                                       boolean highlighted)
    {
        if (highlighted)
        {
            graphics.fill(x - 1, y - 1, right + 1, bottom + 1, HIGHLIGHT_OUTLINE);
        }
    }
}

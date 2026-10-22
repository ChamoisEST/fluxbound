package io.github.chamoisest.fluxbound.client.gui.arrivalanchor;

import io.github.chamoisest.fluxbound.client.gui.DimensionNames;
import io.github.chamoisest.fluxbound.client.gui.ScreenText;
import io.github.chamoisest.fluxbound.client.gui.tab.MachineTab;
import io.github.chamoisest.fluxbound.client.gui.tab.MachineTabType;
import io.github.chamoisest.fluxbound.menu.arrivalanchor.ArrivalAnchorMenu;
import io.github.chamoisest.fluxbound.network.menu.MachineOverviewPayload;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;

// Overview of an Arrival Anchor: owner, size and location (energy is in the energy bar's tooltip). Reads the
// menu's latest Overview payload every frame, so values update live while the screen is open.
public class ArrivalAnchorOverviewTab implements MachineTab
{
    private static final int LINE_SPACING = 1;
    private static final int LINE_COUNT = 4;
    private static final int LABEL_COLOR = 0xFF555555;
    private static final int VALUE_COLOR = 0xFF404040;

    private final ArrivalAnchorMenu menu;

    public ArrivalAnchorOverviewTab(ArrivalAnchorMenu menu)
    {
        this.menu = menu;
    }

    @Override
    public MachineTabType type()
    {
        return MachineTabType.OVERVIEW;
    }

    @Override
    public Component help()
    {
        return text("overview.help");
    }

    @Override
    public int contentHeight(Font font, int width)
    {
        int lines = menu.getOverview() == null ? 1 : LINE_COUNT;
        return lines * (font.lineHeight + LINE_SPACING);
    }

    @Override
    public void extract(GuiGraphicsExtractor graphics, Font font, int x, int y, int width, int mouseX, int mouseY)
    {
        MachineOverviewPayload overview = menu.getOverview();

        if (overview == null)
        {
            graphics.text(font, text("loading"), x, y, LABEL_COLOR, false);
            return;
        }

        int step = font.lineHeight + LINE_SPACING;
        int lineY = y;
        BlockPos center = overview.center();
        Component owner = overview.ownerName().isEmpty()
                ? text("overview.owner.none")
                : Component.literal(overview.ownerName());

        line(graphics, font, x, lineY, width, text("overview.owner"), owner);
        lineY += step;
        line(graphics, font, x, lineY, width, text("overview.blocks"),
                Component.literal(Integer.toString(overview.memberCount())));
        lineY += step;
        line(graphics, font, x, lineY, width, text("overview.dimension"), DimensionNames.of(overview.dimension()));
        lineY += step;
        line(graphics, font, x, lineY, width, text("overview.position"),
                Component.literal(center.getX() + ", " + center.getY() + ", " + center.getZ()));
    }

    // Draws "label value", shortening the value if needed (for example an owner shown as a UUID).
    private static void line(GuiGraphicsExtractor graphics, Font font, int x, int y, int width,
                             Component label, Component value)
    {
        int valueX = x + font.width(label);

        graphics.text(font, label, x, y, LABEL_COLOR, false);
        graphics.text(font, ScreenText.fitToWidth(font, value, x + width - valueX), valueX, y, VALUE_COLOR, false);
    }

    private static Component text(String key)
    {
        return Component.translatable("gui.fluxbound." + key);
    }
}

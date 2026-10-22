package io.github.chamoisest.fluxbound.client.gui.tab;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

// A tab that only describes its section until its controls exist (placeholder policy, decisions.md).
public class PlaceholderTab implements MachineTab
{
    private static final int LINE_SPACING = 1;
    private static final int TEXT_COLOR = 0xFF404040;

    private final MachineTabType type;
    private final Component help;
    private final Component description;

    public PlaceholderTab(MachineTabType type, Component help, Component description)
    {
        this.type = type;
        this.help = help;
        this.description = description;
    }

    @Override
    public MachineTabType type()
    {
        return type;
    }

    @Override
    public Component help()
    {
        return help;
    }

    @Override
    public int contentHeight(Font font, int width)
    {
        return font.split(description, width).size() * (font.lineHeight + LINE_SPACING);
    }

    @Override
    public void extract(GuiGraphicsExtractor graphics, Font font, int x, int y, int width, int mouseX, int mouseY)
    {
        int lineY = y;

        for (FormattedCharSequence line : font.split(description, width))
        {
            graphics.text(font, line, x, lineY, TEXT_COLOR, false);
            lineY += font.lineHeight + LINE_SPACING;
        }
    }
}

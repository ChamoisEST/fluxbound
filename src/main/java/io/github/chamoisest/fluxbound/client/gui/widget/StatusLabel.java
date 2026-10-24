package io.github.chamoisest.fluxbound.client.gui.widget;

import io.github.chamoisest.fluxbound.client.gui.MachineStatus;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;

import java.util.function.Supplier;

// A machine screen's status text, right-aligned at a fixed right edge, with its explanation as tooltip (M1 B9).
// Without a status it is inactive: nothing is drawn and Tab skips it.
public final class StatusLabel extends InfoWidget
{
    private final Font font;
    private final int right;
    private final Supplier<@Nullable MachineStatus> status;

    public StatusLabel(Font font, int right, int y, Supplier<@Nullable MachineStatus> status)
    {
        super(right, y, 0, font.lineHeight, CommonComponents.EMPTY);
        this.font = font;
        this.right = right;
        this.status = status;
        refresh();
    }

    // The text can change, so size, position and narration name follow it; hovering lags by one frame at most.
    private @Nullable MachineStatus refresh()
    {
        MachineStatus current = status.get();
        active = current != null;

        if (current != null)
        {
            setWidth(font.width(current.text()));
            setX(right - getWidth());
            setMessage(current.text());
        }

        return current;
    }

    @Override
    protected @Nullable Component tooltipContent()
    {
        MachineStatus current = status.get();
        return current == null ? null : current.help();
    }

    @Override
    protected void extractContent(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a)
    {
        MachineStatus current = refresh();

        if (current != null)
        {
            graphics.text(font, current.text(), getX(), getY(), current.color(), false);
        }
    }
}

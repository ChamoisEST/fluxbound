package io.github.chamoisest.fluxbound.client.gui.widget;

import io.github.chamoisest.fluxbound.client.gui.OverlayHost;
import io.github.chamoisest.fluxbound.client.gui.ScreenOverlay;
import io.github.chamoisest.fluxbound.client.gui.ScreenText;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.InputWithModifiers;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

// A compact choice field: shows the current value and, when pressed, opens a list of all choices over the
// screen (M1 Q7, B5-5). It never changes its own value: the owner supplies the value to show and decides what a
// choice does, so the field always shows the synced server state (M1 B5-1).
public class DropdownWidget<T> extends AbstractButton
{
    // The closed field in the GUI's own colours: the frame's light grey, slightly raised with a 1 px bevel like
    // vanilla panels, so it reads as a control without standing out (M1 B5-5, user decision). The open list stays a
    // dark popup.
    private static final int FIELD_FILL = 0xFFC6C6C6;
    private static final int BEVEL_LIGHT = 0xFFFFFFFF;
    private static final int BEVEL_DARK = 0xFF373737;
    private static final int FIELD_TEXT_COLOR = 0xFF404040;
    // Drawn one pixel outside the field, over the mid-grey content area, when hovered, focused or open.
    private static final int HIGHLIGHT_OUTLINE = 0xFFFFFFFF;

    private static final int TEXT_PADDING = 4;
    private static final int ARROW_WIDTH = 5;
    private static final int ROW_HEIGHT = 12;
    private static final int TEXT_COLOR = 0xFFFFFFFF;
    private static final int CURRENT_COLOR = 0xFFFFFF55;

    // The open list is a dark popup, unlike the light fields it may cover.
    private static final int LIST_BACKGROUND = 0xF0101010;
    private static final int LIST_BORDER = 0xFFA0A0A0;
    private static final int ROW_HIGHLIGHT = 0x40FFFFFF;

    private final Font font;
    private final List<T> choices;
    private final Function<T, Component> label;
    private final Supplier<T> current;
    private final Consumer<T> onChoose;
    private final OverlayHost host;
    private @Nullable ChoiceList list;

    // The current value may be missing from the choices (for example a stored Team mode); it is shown anyway.
    public DropdownWidget(Font font, int width, int height, List<T> choices, Function<T, Component> label,
                          Supplier<T> current, Consumer<T> onChoose, OverlayHost host)
    {
        super(0, 0, width, height, CommonComponents.EMPTY);
        this.font = font;
        this.choices = List.copyOf(choices);
        this.label = label;
        this.current = current;
        this.onChoose = onChoose;
        this.host = host;
    }

    @Override
    public void onPress(InputWithModifiers input)
    {
        list = new ChoiceList();
        host.openOverlay(list);
    }

    @Override
    protected void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a)
    {
        int right = getRight();
        int bottom = getBottom();

        if (highlighted())
        {
            graphics.fill(getX() - 1, getY() - 1, right + 1, bottom + 1, HIGHLIGHT_OUTLINE);
        }

        // Dark, then white over all but the last column and row, then the fill: white top/left, dark bottom/right.
        graphics.fill(getX(), getY(), right, bottom, BEVEL_DARK);
        graphics.fill(getX(), getY(), right - 1, bottom - 1, BEVEL_LIGHT);
        graphics.fill(getX() + 1, getY() + 1, right - 1, bottom - 1, FIELD_FILL);

        int textWidth = width - 3 * TEXT_PADDING - ARROW_WIDTH;
        int textY = getY() + (height - font.lineHeight) / 2 + 1;

        graphics.text(font, ScreenText.fitToWidth(font, label.apply(current.get()), textWidth),
                getX() + TEXT_PADDING, textY, FIELD_TEXT_COLOR, false);
        extractArrow(graphics, getRight() - TEXT_PADDING - ARROW_WIDTH, getY() + (height - 3) / 2);
    }

    // Hovered, focused, or its list is open, so it is clear which setting is being changed.
    private boolean highlighted()
    {
        return isHoveredOrFocused() || (list != null && host.isOpen(list));
    }

    // A small downward triangle drawn from three lines, so no extra sprite is needed.
    private static void extractArrow(GuiGraphicsExtractor graphics, int x, int y)
    {
        graphics.fill(x, y, x + 5, y + 1, FIELD_TEXT_COLOR);
        graphics.fill(x + 1, y + 1, x + 4, y + 2, FIELD_TEXT_COLOR);
        graphics.fill(x + 2, y + 2, x + 3, y + 3, FIELD_TEXT_COLOR);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output)
    {
        output.add(NarratedElementType.TITLE, label.apply(current.get()));
    }

    // The open list: a dark popup below the field, or above it when there is no room below; the current value is
    // yellow. Arrow keys move the highlight, Enter or Space chooses.
    private final class ChoiceList implements ScreenOverlay
    {
        private final int top;
        private int highlighted;

        private ChoiceList()
        {
            int listHeight = choices.size() * ROW_HEIGHT;
            int screenHeight = Minecraft.getInstance().getWindow().getGuiScaledHeight();

            this.top = getBottom() + listHeight <= screenHeight ? getBottom() : getY() - listHeight;
            this.highlighted = Math.max(0, choices.indexOf(current.get()));
        }

        @Override
        public void extract(GuiGraphicsExtractor graphics, int mouseX, int mouseY)
        {
            int hoveredRow = rowAt(mouseX, mouseY);

            if (hoveredRow >= 0)
            {
                highlighted = hoveredRow;
            }

            T currentValue = current.get();
            int bottom = top + choices.size() * ROW_HEIGHT;

            // The border lies one pixel outside the rows, so the list keeps exactly the field's width.
            graphics.fill(getX() - 1, top - 1, getRight() + 1, bottom + 1, LIST_BORDER);
            graphics.fill(getX(), top, getRight(), bottom, LIST_BACKGROUND);

            for (int row = 0; row < choices.size(); row++)
            {
                int rowY = top + row * ROW_HEIGHT;
                T choice = choices.get(row);

                if (row == highlighted)
                {
                    graphics.fill(getX(), rowY, getRight(), rowY + ROW_HEIGHT, ROW_HIGHLIGHT);
                }

                graphics.text(font, ScreenText.fitToWidth(font, label.apply(choice), width - 2 * TEXT_PADDING),
                        getX() + TEXT_PADDING, rowY + 2, choice.equals(currentValue) ? CURRENT_COLOR : TEXT_COLOR,
                        true);
            }
        }

        @Override
        public boolean isMouseOver(double mouseX, double mouseY)
        {
            return rowAt(mouseX, mouseY) >= 0;
        }

        @Override
        public void mouseClicked(MouseButtonEvent event)
        {
            int row = rowAt(event.x(), event.y());

            if (row >= 0)
            {
                choose(row);
            }
        }

        @Override
        public boolean keyPressed(KeyEvent event)
        {
            if (event.isUp())
            {
                highlighted = Math.max(0, highlighted - 1);
                return true;
            }

            if (event.isDown())
            {
                highlighted = Math.min(choices.size() - 1, highlighted + 1);
                return true;
            }

            if (event.isSelection())
            {
                choose(highlighted);
                return true;
            }

            return false;
        }

        private void choose(int row)
        {
            T choice = choices.get(row);

            host.closeOverlay();
            playDownSound(Minecraft.getInstance().getSoundManager());

            if (!choice.equals(current.get()))
            {
                onChoose.accept(choice);
            }
        }

        private int rowAt(double mouseX, double mouseY)
        {
            if (mouseX < getX() || mouseX >= getRight() || mouseY < top)
            {
                return -1;
            }

            int row = (int) (mouseY - top) / ROW_HEIGHT;
            return row < choices.size() ? row : -1;
        }
    }
}

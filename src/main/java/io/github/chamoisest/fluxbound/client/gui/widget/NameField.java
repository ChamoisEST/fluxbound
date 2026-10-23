package io.github.chamoisest.fluxbound.client.gui.widget;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.util.StringUtil;
import org.jspecify.annotations.Nullable;

import java.util.Collection;
import java.util.function.Supplier;

// A text field for a player name, sunken in the GUI's colours (M1 B6b-2). It completes the names of online
// players as grey ghost text (M1 B6-2): Tab, or → with the cursor at the end, accepts the completion; Enter
// submits. Only names the server would accept can be typed.
public class NameField extends EditBox
{
    // Vanilla's longest player name; the filter below only sees inserted text, not the whole value.
    private static final int MAX_LENGTH = 16;

    // Text inset inside the frame, as in vanilla's bordered field. Vanilla's border is switched off and this
    // frame drawn instead, so the widget itself covers only the text area and vanilla places text and cursor.
    private static final int PADDING_X = 4;
    private static final int PADDING_Y = 3;

    private final int frameHeight;
    private final Supplier<? extends Collection<String>> candidates;
    private final Runnable onSubmit;
    private @Nullable String completion;

    // onEdit runs after every change of the text, including accepting a completion.
    public NameField(Font font, int frameHeight, Component hint, Supplier<? extends Collection<String>> candidates,
                     Runnable onEdit, Runnable onSubmit)
    {
        super(font, 0, font.lineHeight, hint);
        this.frameHeight = frameHeight;
        this.candidates = candidates;
        this.onSubmit = onSubmit;

        setBordered(false);
        setMaxLength(MAX_LENGTH);
        setFilter(StringUtil::isValidPlayerName);
        setTextColor(FieldStyle.TEXT_COLOR);
        setTextShadow(false);
        // Vanilla always draws the hint with a shadow, which is meant for its dark fields and looks heavy on this
        // light one; a transparent shadow colour in the style overrides that.
        setHint(hint.copy().withColor(FieldStyle.HINT_COLOR).withStyle(Style::withoutShadow));
        setResponder(value ->
        {
            updateCompletion();
            onEdit.run();
        });
    }

    // Positions the frame; the text area inside it follows.
    public void placeFrame(int x, int y, int frameWidth)
    {
        setX(x + PADDING_X);
        setY(y + PADDING_Y);
        setWidth(frameWidth - 2 * PADDING_X);
    }

    // The whole frame counts, so clicking its padding also focuses the field.
    @Override
    public boolean isMouseOver(double mouseX, double mouseY)
    {
        return isActive() && inFrame(mouseX, mouseY);
    }

    @Override
    public void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a)
    {
        if (isVisible())
        {
            boolean hovered = graphics.containsPointInScissor(mouseX, mouseY) && inFrame(mouseX, mouseY);
            FieldStyle.extractSunken(graphics, frameX(), frameY(), frameRight(), frameY() + frameHeight,
                    hovered || isFocused());
        }

        super.extractWidgetRenderState(graphics, mouseX, mouseY, a);
    }

    @Override
    public boolean keyPressed(KeyEvent event)
    {
        if (isActive() && isFocused())
        {
            if (event.isConfirmation())
            {
                onSubmit.run();
                return true;
            }

            boolean atEnd = getCursorPosition() == getValue().length();

            if (completion != null && atEnd && (event.isCycleFocus() || event.isRight()))
            {
                setValue(completion);
                return true;
            }
        }

        return super.keyPressed(event);
    }

    // The alphabetically first candidate that starts with the typed text, ignoring case.
    private void updateCompletion()
    {
        String value = getValue();

        completion = value.isEmpty() ? null : candidates.get().stream()
                .filter(name -> name.length() > value.length() && name.regionMatches(true, 0, value, 0, value.length()))
                .min(String.CASE_INSENSITIVE_ORDER)
                .orElse(null);

        setSuggestion(completion == null ? null : completion.substring(value.length()));
    }

    private boolean inFrame(double mouseX, double mouseY)
    {
        return mouseX >= frameX() && mouseX < frameRight() && mouseY >= frameY() && mouseY < frameY() + frameHeight;
    }

    private int frameX()
    {
        return getX() - PADDING_X;
    }

    private int frameY()
    {
        return getY() - PADDING_Y;
    }

    private int frameRight()
    {
        return getRight() + PADDING_X;
    }
}

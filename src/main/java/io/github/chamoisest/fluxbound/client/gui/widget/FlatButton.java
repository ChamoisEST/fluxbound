package io.github.chamoisest.fluxbound.client.gui.widget;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.InputWithModifiers;
import net.minecraft.network.chat.Component;

// A small button in the GUI's own colours, raised like the dropdown field, for rows too low for vanilla's 20 px
// button sprite (M1 B6b-2). Greyed out while inactive.
public class FlatButton extends AbstractButton
{
    private final Font font;
    private final Runnable onPress;

    public FlatButton(Font font, int width, int height, Component label, Runnable onPress)
    {
        super(0, 0, width, height, label);
        this.font = font;
        this.onPress = onPress;
    }

    @Override
    public void onPress(InputWithModifiers input)
    {
        onPress.run();
    }

    @Override
    protected void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a)
    {
        FieldStyle.extractRaised(graphics, getX(), getY(), getRight(), getBottom(), active && isHoveredOrFocused());
        graphics.text(font, getMessage(), getX() + (width - font.width(getMessage())) / 2,
                getY() + (height - font.lineHeight) / 2 + 1,
                active ? FieldStyle.TEXT_COLOR : FieldStyle.DISABLED_TEXT_COLOR, false);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output)
    {
        defaultButtonNarrationText(output);
    }
}

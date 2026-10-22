package io.github.chamoisest.fluxbound.client.gui;

import net.minecraft.client.gui.Font;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.util.FormattedCharSequence;

// Text helpers shared by machine screens.
public final class ScreenText
{
    private ScreenText() {}

    // Shortens text to one line, ending in an ellipsis when it doesn't fit (for example, long translations).
    public static FormattedCharSequence fitToWidth(Font font, Component text, int width)
    {
        if (font.width(text) <= width)
        {
            return text.getVisualOrderText();
        }

        FormattedText cut = font.substrByWidth(text, width - font.width(CommonComponents.ELLIPSIS));
        return Language.getInstance().getVisualOrder(FormattedText.composite(cut, CommonComponents.ELLIPSIS));
    }
}

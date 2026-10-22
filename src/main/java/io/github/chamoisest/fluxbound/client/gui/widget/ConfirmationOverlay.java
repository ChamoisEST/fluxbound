package io.github.chamoisest.fluxbound.client.gui.widget;

import io.github.chamoisest.fluxbound.Fluxbound;
import io.github.chamoisest.fluxbound.client.gui.OverlayHost;
import io.github.chamoisest.fluxbound.client.gui.ScreenOverlay;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;

import java.util.List;

// A small modal question over the machine screen with Proceed and Cancel (M1 B5-4). The rest of the screen is
// dimmed. Cancel is preselected for the keyboard (left/right or Tab switch, Enter or Space presses); Esc, the
// inventory key and a click outside also cancel, because the screen then closes the overlay.
public class ConfirmationOverlay implements ScreenOverlay
{
    private static final Identifier FRAME_SPRITE = Identifier.fromNamespaceAndPath(Fluxbound.MODID, "frame");
    private static final Identifier BUTTON_SPRITE = Identifier.withDefaultNamespace("widget/button");
    private static final Identifier BUTTON_HIGHLIGHTED_SPRITE = Identifier.withDefaultNamespace("widget/button_highlighted");

    private static final int WIDTH = 180;
    private static final int PADDING = 8;
    private static final int LINE_SPACING = 1;
    private static final int TITLE_GAP = 4;
    private static final int BUTTON_TOP_GAP = 6;
    private static final int BUTTON_WIDTH = 70;
    private static final int BUTTON_HEIGHT = 16;
    private static final int BUTTON_GAP = 8;
    private static final int DIM_COLOR = 0x80000000;
    private static final int TITLE_COLOR = 0xFFAA0000;
    private static final int TEXT_COLOR = 0xFF404040;
    private static final int BUTTON_TEXT_COLOR = 0xFFFFFFFF;

    private static final int PROCEED = 0;
    private static final int CANCEL = 1;

    private final Font font;
    private final Component title = Component.translatable("gui.fluxbound.confirm.title");
    private final List<FormattedCharSequence> lines;
    private final Runnable onConfirm;
    private final OverlayHost host;
    private final int left;
    private final int top;
    private final int height;
    private int selected = CANCEL;

    public ConfirmationOverlay(Font font, Component message, Runnable onConfirm, OverlayHost host)
    {
        this.font = font;
        this.lines = font.split(message, WIDTH - 2 * PADDING);
        this.onConfirm = onConfirm;
        this.host = host;
        this.height = PADDING + font.lineHeight + TITLE_GAP + lines.size() * (font.lineHeight + LINE_SPACING)
                + BUTTON_TOP_GAP + BUTTON_HEIGHT + PADDING;

        Minecraft minecraft = Minecraft.getInstance();
        this.left = (minecraft.getWindow().getGuiScaledWidth() - WIDTH) / 2;
        this.top = (minecraft.getWindow().getGuiScaledHeight() - height) / 2;
    }

    @Override
    public void extract(GuiGraphicsExtractor graphics, int mouseX, int mouseY)
    {
        Minecraft minecraft = Minecraft.getInstance();
        graphics.fill(0, 0, minecraft.getWindow().getGuiScaledWidth(), minecraft.getWindow().getGuiScaledHeight(),
                DIM_COLOR);
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, FRAME_SPRITE, left, top, WIDTH, height);

        int textY = top + PADDING;
        graphics.text(font, title, left + PADDING, textY, TITLE_COLOR, false);
        textY += font.lineHeight + TITLE_GAP;

        for (FormattedCharSequence line : lines)
        {
            graphics.text(font, line, left + PADDING, textY, TEXT_COLOR, false);
            textY += font.lineHeight + LINE_SPACING;
        }

        int hoveredButton = buttonAt(mouseX, mouseY);

        if (hoveredButton >= 0)
        {
            selected = hoveredButton;
        }

        extractButton(graphics, PROCEED, CommonComponents.GUI_PROCEED);
        extractButton(graphics, CANCEL, CommonComponents.GUI_CANCEL);
    }

    private void extractButton(GuiGraphicsExtractor graphics, int button, Component label)
    {
        int x = buttonX(button);
        int y = buttonY();

        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, button == selected ? BUTTON_HIGHLIGHTED_SPRITE : BUTTON_SPRITE,
                x, y, BUTTON_WIDTH, BUTTON_HEIGHT);
        graphics.text(font, label, x + (BUTTON_WIDTH - font.width(label)) / 2,
                y + (BUTTON_HEIGHT - font.lineHeight) / 2 + 1, BUTTON_TEXT_COLOR, true);
    }

    @Override
    public boolean isMouseOver(double mouseX, double mouseY)
    {
        return mouseX >= left && mouseX < left + WIDTH && mouseY >= top && mouseY < top + height;
    }

    @Override
    public void mouseClicked(MouseButtonEvent event)
    {
        int button = buttonAt(event.x(), event.y());

        if (button >= 0)
        {
            press(button);
        }
    }

    @Override
    public boolean keyPressed(KeyEvent event)
    {
        if (event.isLeft() || event.isRight() || event.isCycleFocus())
        {
            selected = selected == PROCEED ? CANCEL : PROCEED;
            return true;
        }

        if (event.isSelection())
        {
            press(selected);
            return true;
        }

        return false;
    }

    // The overlay closes first, so a confirmation that opens another overlay would not be closed by it.
    private void press(int button)
    {
        host.closeOverlay();
        AbstractWidget.playButtonClickSound(Minecraft.getInstance().getSoundManager());

        if (button == PROCEED)
        {
            onConfirm.run();
        }
    }

    private int buttonAt(double mouseX, double mouseY)
    {
        if (mouseY < buttonY() || mouseY >= buttonY() + BUTTON_HEIGHT)
        {
            return -1;
        }

        for (int button = PROCEED; button <= CANCEL; button++)
        {
            if (mouseX >= buttonX(button) && mouseX < buttonX(button) + BUTTON_WIDTH)
            {
                return button;
            }
        }

        return -1;
    }

    private int buttonX(int button)
    {
        int rowLeft = left + (WIDTH - 2 * BUTTON_WIDTH - BUTTON_GAP) / 2;
        return rowLeft + button * (BUTTON_WIDTH + BUTTON_GAP);
    }

    private int buttonY()
    {
        return top + height - PADDING - BUTTON_HEIGHT;
    }
}

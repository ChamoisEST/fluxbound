package io.github.chamoisest.fluxbound.client.gui.tab;

import io.github.chamoisest.fluxbound.Fluxbound;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ComponentPath;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.navigation.FocusNavigationEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.function.IntConsumer;

// A row of tabs. When the tabs are wider than the bar, arrows at both ends and the mouse wheel shift
// the visible tabs one at a time. The selected tab is coloured by its type; inactive tabs are grey with
// a muted stripe of their colour, which becomes full when hovered (M1 decision E1).
public class MachineTabBar extends AbstractWidget
{
    // Height of the tab row above the frame. All tabs end at the frame's top edge without covering it.
    public static final int HEIGHT = 18;

    private static final Identifier TAB_SPRITE = sprite("tab");
    private static final Identifier TAB_HOVERED_SPRITE = sprite("tab_hovered");
    private static final Identifier TAB_SELECTED_SPRITE = sprite("tab_selected");
    private static final Identifier TAB_STRIPE_SPRITE = sprite("tab_stripe");
    private static final Identifier LEFT_SPRITE = sprite("tab_arrow_left");
    private static final Identifier LEFT_HOVERED_SPRITE = sprite("tab_arrow_left_hovered");
    private static final Identifier LEFT_DISABLED_SPRITE = sprite("tab_arrow_left_disabled");
    private static final Identifier RIGHT_SPRITE = sprite("tab_arrow_right");
    private static final Identifier RIGHT_HOVERED_SPRITE = sprite("tab_arrow_right_hovered");
    private static final Identifier RIGHT_DISABLED_SPRITE = sprite("tab_arrow_right_disabled");

    private static final int TAB_PADDING = 6;
    private static final int TAB_GAP = 1;
    private static final int ARROW_WIDTH = 9;

    // Inactive tabs are lower than the selected tab; both stand on the frame's outline.
    private static final int INACTIVE_OFFSET = 3;
    private static final int INACTIVE_HEIGHT = HEIGHT - INACTIVE_OFFSET;
    private static final int STRIPE_HEIGHT = 2;
    private static final int INACTIVE_GREY = 0xFF969696;
    private static final int TEXT_COLOR = 0xFF404040;
    private static final int SELECTED_TEXT_COLOR = 0xFFFFFFFF;

    private final Font font;
    private final List<MachineTab> tabs;
    private final IntConsumer onSelect;
    private int selected;
    private int firstVisible;

    public MachineTabBar(Font font, int x, int y, int width, List<MachineTab> tabs, int selected,
                         IntConsumer onSelect)
    {
        super(x, y, width, HEIGHT, CommonComponents.EMPTY);
        this.font = font;
        this.tabs = List.copyOf(tabs);
        this.selected = selected;
        this.onSelect = onSelect;
        ensureVisible(selected);
    }

    private static Identifier sprite(String name)
    {
        return Identifier.fromNamespaceAndPath(Fluxbound.MODID, name);
    }

    @Override
    protected void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a)
    {
        int hoveredTab = isHovered() ? tabAt(mouseX) : -1;

        graphics.enableScissor(tabAreaLeft(), getY(), tabAreaRight(), getBottom());

        for (int index = firstVisible; index < tabs.size(); index++)
        {
            int left = tabLeft(index);

            if (left >= tabAreaRight())
            {
                break;
            }

            extractTab(graphics, index, left, index == hoveredTab);
        }

        graphics.disableScissor();

        if (overflowing())
        {
            boolean overLeft = isHovered() && mouseX < getX() + ARROW_WIDTH;
            boolean overRight = isHovered() && mouseX >= getRight() - ARROW_WIDTH;

            extractArrow(graphics, getX(), firstVisible > 0, overLeft,
                    LEFT_SPRITE, LEFT_HOVERED_SPRITE, LEFT_DISABLED_SPRITE);
            extractArrow(graphics, getRight() - ARROW_WIDTH, canScrollRight(), overRight,
                    RIGHT_SPRITE, RIGHT_HOVERED_SPRITE, RIGHT_DISABLED_SPRITE);
        }
    }

    private void extractTab(GuiGraphicsExtractor graphics, int index, int left, boolean hovered)
    {
        MachineTabType type = tabs.get(index).type();
        int width = tabWidth(index);

        if (index == selected)
        {
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, TAB_SELECTED_SPRITE, left, getY(), width, HEIGHT,
                    type.color());
            graphics.text(font, type.label(), left + TAB_PADDING, getY() + 5, SELECTED_TEXT_COLOR, false);
            return;
        }

        int top = getY() + INACTIVE_OFFSET;
        int stripeColor = hovered ? type.color() : ARGB.srgbLerp(0.25F, type.color(), INACTIVE_GREY);

        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, hovered ? TAB_HOVERED_SPRITE : TAB_SPRITE,
                left, top, width, INACTIVE_HEIGHT);
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, TAB_STRIPE_SPRITE, left + 1, top + 1, width - 2,
                STRIPE_HEIGHT, stripeColor);
        graphics.text(font, type.label(), left + TAB_PADDING, top + 4, TEXT_COLOR, false);
    }

    private void extractArrow(GuiGraphicsExtractor graphics, int x, boolean enabled, boolean hovered,
                              Identifier normal, Identifier hoveredSprite, Identifier disabled)
    {
        Identifier sprite = !enabled ? disabled : hovered ? hoveredSprite : normal;
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, x, getY() + INACTIVE_OFFSET, ARROW_WIDTH,
                INACTIVE_HEIGHT);
    }

    // Plays the click sound only when the click actually does something.
    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick)
    {
        if (!isActive() || !isValidClickButton(event.buttonInfo()) || !isMouseOver(event.x(), event.y()))
        {
            return false;
        }

        if (!handleClick((int) event.x()))
        {
            return false;
        }

        playDownSound(Minecraft.getInstance().getSoundManager());
        return true;
    }

    private boolean handleClick(int mouseX)
    {
        if (overflowing())
        {
            if (mouseX < getX() + ARROW_WIDTH)
            {
                return scrollBy(-1);
            }

            if (mouseX >= getRight() - ARROW_WIDTH)
            {
                return scrollBy(1);
            }
        }

        int index = tabAt(mouseX);

        if (index < 0)
        {
            return false;
        }

        select(index);
        return true;
    }

    // Ctrl+Tab and Ctrl+Shift+Tab cycle the tabs and Ctrl+1…9 (0 = tenth) jump to one, like vanilla's
    // TabNavigationBar; a digit beyond the last tab selects the last one. Cmd instead of Ctrl on macOS.
    public boolean selectByKey(KeyEvent event)
    {
        if (!event.hasControlDownWithQuirk())
        {
            return false;
        }

        int digit = event.getDigit();
        int index;

        if (digit != -1)
        {
            index = Math.min(Math.floorMod(digit - 1, 10), tabs.size() - 1);
        }
        else if (event.isCycleFocus())
        {
            index = Math.floorMod(selected + (event.hasShiftDown() ? -1 : 1), tabs.size());
        }
        else
        {
            return false;
        }

        select(index);
        playDownSound(Minecraft.getInstance().getSoundManager());
        return true;
    }

    private void select(int index)
    {
        if (index != selected)
        {
            selected = index;
            onSelect.accept(index);
        }

        ensureVisible(index);
    }

    // Not a Tab stop: tabs are switched with Ctrl+Tab and Ctrl+1…9 from anywhere in the screen (M1 B9).
    @Override
    public @Nullable ComponentPath nextFocusPath(FocusNavigationEvent event)
    {
        return null;
    }

    @Override
    public boolean mouseScrolled(double x, double y, double scrollX, double scrollY)
    {
        return scrollY != 0 && scrollBy(scrollY > 0 ? -1 : 1);
    }

    private boolean scrollBy(int direction)
    {
        if (direction < 0 && firstVisible > 0)
        {
            firstVisible--;
            return true;
        }

        if (direction > 0 && canScrollRight())
        {
            firstVisible++;
            return true;
        }

        return false;
    }

    private int tabWidth(int index)
    {
        return font.width(tabs.get(index).type().label()) + 2 * TAB_PADDING;
    }

    private boolean overflowing()
    {
        int total = -TAB_GAP;

        for (int index = 0; index < tabs.size(); index++)
        {
            total += tabWidth(index) + TAB_GAP;
        }

        return total > width;
    }

    private int tabAreaLeft()
    {
        return overflowing() ? getX() + ARROW_WIDTH + TAB_GAP : getX();
    }

    private int tabAreaRight()
    {
        return overflowing() ? getRight() - ARROW_WIDTH - TAB_GAP : getRight();
    }

    // Only valid for index >= firstVisible.
    private int tabLeft(int index)
    {
        int left = tabAreaLeft();

        for (int current = firstVisible; current < index; current++)
        {
            left += tabWidth(current) + TAB_GAP;
        }

        return left;
    }

    private boolean canScrollRight()
    {
        return tabLeft(tabs.size() - 1) + tabWidth(tabs.size() - 1) > tabAreaRight();
    }

    private void ensureVisible(int index)
    {
        firstVisible = Math.min(firstVisible, index);

        while (firstVisible < index && tabLeft(index) + tabWidth(index) > tabAreaRight())
        {
            firstVisible++;
        }
    }

    private int tabAt(int mouseX)
    {
        if (mouseX < tabAreaLeft() || mouseX >= tabAreaRight())
        {
            return -1;
        }

        for (int index = firstVisible; index < tabs.size(); index++)
        {
            int left = tabLeft(index);

            if (mouseX >= left && mouseX < left + tabWidth(index))
            {
                return index;
            }
        }

        return -1;
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output)
    {
        output.add(NarratedElementType.TITLE, tabs.get(selected).type().label());
    }
}

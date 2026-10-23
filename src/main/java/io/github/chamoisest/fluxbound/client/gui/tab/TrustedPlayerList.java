package io.github.chamoisest.fluxbound.client.gui.tab;

import com.mojang.blaze3d.platform.InputConstants;
import io.github.chamoisest.fluxbound.client.gui.ScreenText;
import io.github.chamoisest.fluxbound.network.menu.MachinePermissionsPayload.TrustedPlayer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.PlayerFaceExtractor;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.PlayerSkin;
import net.minecraft.world.item.component.ResolvableProfile;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Supplier;

// The rows of the Trusted Players tab as one widget (M1 B6b-1 c): face, name and a remove "×" per row. With
// keyboard focus, ↑/↓ select a row and Delete or Backspace removes it; Enter and Space don't, because removing
// is destructive. The selection follows the player, not the position, so a list change never moves it onto
// someone else.
public class TrustedPlayerList extends AbstractWidget implements ClippedWidget
{
    public static final int ROW_HEIGHT = 12;

    private static final int FACE_SIZE = 8;
    private static final int FACE_GAP = 4;
    private static final int NAME_GAP = 2;
    private static final int CROSS_SIZE = 5;
    // Clickable width of the "×" at the right end of each row.
    private static final int REMOVE_WIDTH = 11;
    private static final int TOOLTIP_WIDTH = 170;
    private static final int NAME_COLOR = 0xFF404040;
    private static final int UNKNOWN_COLOR = 0xFF808080;
    private static final int REMOVE_COLOR = 0xFF555555;
    private static final int REMOVE_HOVER_COLOR = 0xFFAA0000;
    private static final int SELECTED_FILL = 0x40FFFFFF;

    private final Font font;
    private final Supplier<List<TrustedPlayer>> players;
    private final Consumer<TrustedPlayer> onRemove;
    private int viewportTop;
    private int viewportBottom;
    private @Nullable ScrollTarget scroll;
    // The selected player's stored ID, and their last row for when they leave the list.
    private @Nullable String selectedId;
    private int selectedRow;

    public TrustedPlayerList(Font font, Supplier<List<TrustedPlayer>> players, Consumer<TrustedPlayer> onRemove)
    {
        super(0, 0, 0, 0, Component.translatable("gui.fluxbound.tab.trusted_players"));
        this.font = font;
        this.players = players;
        this.onRemove = onRemove;
    }

    // Called by the tab every frame; the height follows the number of players.
    public void place(int x, int y, int width)
    {
        setX(x);
        setY(y);
        setSize(width, players.get().size() * ROW_HEIGHT);
    }

    @Override
    public void setViewport(int top, int bottom, ScrollTarget scroll)
    {
        this.viewportTop = top;
        this.viewportBottom = bottom;
        this.scroll = scroll;
    }

    // Clicks only count in the visible part.
    @Override
    public boolean isMouseOver(double mouseX, double mouseY)
    {
        return super.isMouseOver(mouseX, mouseY) && mouseY >= viewportTop && mouseY < viewportBottom;
    }

    @Override
    public void setFocused(boolean focused)
    {
        super.setFocused(focused);

        if (focused)
        {
            revealSelected();
        }
    }

    @Override
    public boolean keyPressed(KeyEvent event)
    {
        List<TrustedPlayer> rows = players.get();

        if (!isFocused() || rows.isEmpty())
        {
            return false;
        }

        int row = currentRow(rows);

        if (event.isUp() || event.isDown())
        {
            int next = row + (event.isUp() ? -1 : 1);

            // At either end the key moves focus out of the list, like vanilla arrow navigation.
            if (next < 0 || next >= rows.size())
            {
                return false;
            }

            select(rows, next);
            revealSelected();
            return true;
        }

        if (event.key() == InputConstants.KEY_DELETE || event.key() == InputConstants.KEY_BACKSPACE)
        {
            remove(rows.get(row));
            return true;
        }

        return false;
    }

    @Override
    public void onClick(MouseButtonEvent event, boolean doubleClick)
    {
        List<TrustedPlayer> rows = players.get();
        int row = rowAt(event.y());

        if (row < 0 || row >= rows.size())
        {
            return;
        }

        select(rows, row);

        if (event.x() >= removeX())
        {
            remove(rows.get(row));
        }
    }

    // Selecting a row is silent; only removing clicks.
    @Override
    public void playDownSound(SoundManager soundManager)
    {
    }

    @Override
    protected void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a)
    {
        List<TrustedPlayer> rows = players.get();

        if (rows.isEmpty())
        {
            return;
        }

        int selected = isFocused() ? currentRow(rows) : -1;
        int hoveredRow = isHovered() ? rowAt(mouseY) : -1;

        for (int row = 0; row < rows.size(); row++)
        {
            int rowY = getY() + row * ROW_HEIGHT;

            // Rows scrolled out of view are skipped, so faces are only looked up for visible players.
            if (rowY + ROW_HEIGHT <= viewportTop || rowY >= viewportBottom)
            {
                continue;
            }

            if (row == selected)
            {
                graphics.fill(getX(), rowY, getRight(), rowY + ROW_HEIGHT, SELECTED_FILL);
            }

            extractRow(graphics, rows.get(row), rowY, row == hoveredRow, mouseX, mouseY);
        }
    }

    private void extractRow(GuiGraphicsExtractor graphics, TrustedPlayer player, int y, boolean hovered,
                            int mouseX, int mouseY)
    {
        PlayerFaceExtractor.extractRenderState(graphics, skin(player.id()), getX(), y + (ROW_HEIGHT - FACE_SIZE) / 2,
                FACE_SIZE);

        int nameX = getX() + FACE_SIZE + FACE_GAP;
        graphics.text(font, ScreenText.fitToWidth(font, name(player), removeX() - NAME_GAP - nameX), nameX,
                y + (ROW_HEIGHT - font.lineHeight) / 2 + 1, player.name().isPresent() ? NAME_COLOR : UNKNOWN_COLOR,
                false);

        boolean overRemove = hovered && mouseX >= removeX();
        extractCross(graphics, removeX() + (REMOVE_WIDTH - CROSS_SIZE) / 2, y + (ROW_HEIGHT - CROSS_SIZE) / 2,
                overRemove ? REMOVE_HOVER_COLOR : REMOVE_COLOR);

        if (overRemove)
        {
            tooltip(graphics, Component.translatable("gui.fluxbound.trusted_players.remove", name(player)),
                    mouseX, mouseY);
        }
        else if (hovered && player.name().isEmpty())
        {
            tooltip(graphics, Component.translatable("gui.fluxbound.trusted_players.unknown.help", player.id()),
                    mouseX, mouseY);
        }
    }

    // A small "×" from single pixels, so no sprite or font glyph is needed.
    private static void extractCross(GuiGraphicsExtractor graphics, int x, int y, int color)
    {
        for (int step = 0; step < CROSS_SIZE; step++)
        {
            graphics.fill(x + step, y + step, x + step + 1, y + step + 1, color);
            graphics.fill(x + CROSS_SIZE - 1 - step, y + step, x + CROSS_SIZE - step, y + step + 1, color);
        }
    }

    private void tooltip(GuiGraphicsExtractor graphics, Component text, int mouseX, int mouseY)
    {
        graphics.setTooltipForNextFrame(font, font.split(text, TOOLTIP_WIDTH), mouseX, mouseY);
    }

    // The selected player's row; if they are no longer listed, the row at their last position (or the last row).
    private int currentRow(List<TrustedPlayer> rows)
    {
        for (int row = 0; row < rows.size(); row++)
        {
            if (rows.get(row).id().equals(selectedId))
            {
                selectedRow = row;
                return row;
            }
        }

        int row = Math.min(selectedRow, rows.size() - 1);
        select(rows, row);
        return row;
    }

    private void select(List<TrustedPlayer> rows, int row)
    {
        selectedRow = row;
        selectedId = rows.get(row).id();
    }

    private void revealSelected()
    {
        List<TrustedPlayer> rows = players.get();

        if (scroll != null && !rows.isEmpty())
        {
            int top = getY() + currentRow(rows) * ROW_HEIGHT;
            scroll.reveal(top, top + ROW_HEIGHT);
        }
    }

    private void remove(TrustedPlayer player)
    {
        playButtonClickSound(Minecraft.getInstance().getSoundManager());
        onRemove.accept(player);
    }

    private int rowAt(double mouseY)
    {
        return mouseY < getY() ? -1 : (int) (mouseY - getY()) / ROW_HEIGHT;
    }

    private int removeX()
    {
        return getRight() - REMOVE_WIDTH;
    }

    private static Component name(TrustedPlayer player)
    {
        return player.name().<Component>map(Component::literal)
                .orElse(Component.translatable("gui.fluxbound.trusted_players.unknown"));
    }

    // The vanilla skin cache resolves the profile in the background (tab list first, then Mojang) and returns the
    // default skin until it has loaded. A stored ID that is not a UUID can't be looked up and keeps the default.
    private static PlayerSkin skin(String storedId)
    {
        UUID id;

        try
        {
            id = UUID.fromString(storedId);
        }
        catch (IllegalArgumentException exception)
        {
            return DefaultPlayerSkin.getDefaultSkin();
        }

        return Minecraft.getInstance().playerSkinRenderCache()
                .getOrDefault(ResolvableProfile.createUnresolved(id)).playerSkin();
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output)
    {
        List<TrustedPlayer> rows = players.get();
        output.add(NarratedElementType.TITLE, rows.isEmpty() ? getMessage() : name(rows.get(currentRow(rows))));
    }
}

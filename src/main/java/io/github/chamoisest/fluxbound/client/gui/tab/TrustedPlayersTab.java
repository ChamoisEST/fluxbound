package io.github.chamoisest.fluxbound.client.gui.tab;

import io.github.chamoisest.fluxbound.client.gui.ScreenText;
import io.github.chamoisest.fluxbound.network.menu.MachinePermissionsPayload;
import io.github.chamoisest.fluxbound.network.menu.MachinePermissionsPayload.TrustedPlayer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.PlayerFaceExtractor;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.player.PlayerSkin;
import net.minecraft.world.item.component.ResolvableProfile;

import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

// Read-only list of a machine's trusted players with their faces, shared by all machine screens. Names come
// from the server (M1 B4-3); faces load on the client like vanilla player heads (M1 B4-4).
public class TrustedPlayersTab implements MachineTab
{
    private static final int FACE_SIZE = 8;
    private static final int ROW_HEIGHT = 11;
    private static final int FACE_GAP = 4;
    private static final int LINE_SPACING = 1;
    private static final int TOOLTIP_WIDTH = 170;
    private static final int NAME_COLOR = 0xFF404040;
    private static final int UNKNOWN_COLOR = 0xFF808080;
    private static final int MESSAGE_COLOR = 0xFF555555;

    private final Supplier<MachinePermissionsPayload> permissions;

    public TrustedPlayersTab(Supplier<MachinePermissionsPayload> permissions)
    {
        this.permissions = permissions;
    }

    @Override
    public MachineTabType type()
    {
        return MachineTabType.TRUSTED_PLAYERS;
    }

    @Override
    public Component help()
    {
        return text("trusted_players.help");
    }

    @Override
    public int contentHeight(Font font, int width)
    {
        Component message = message();

        if (message != null)
        {
            return font.split(message, width).size() * (font.lineHeight + LINE_SPACING);
        }

        return trusted().size() * ROW_HEIGHT;
    }

    @Override
    public void extract(GuiGraphicsExtractor graphics, Font font, int x, int y, int width, int mouseX, int mouseY)
    {
        Component message = message();

        if (message != null)
        {
            int lineY = y;

            for (FormattedCharSequence line : font.split(message, width))
            {
                graphics.text(font, line, x, lineY, MESSAGE_COLOR, false);
                lineY += font.lineHeight + LINE_SPACING;
            }

            return;
        }

        int rowY = y;

        for (TrustedPlayer player : trusted())
        {
            // Rows scrolled out of view are skipped, so faces are only looked up for visible players.
            if (graphics.containsPointInScissor(x, rowY) || graphics.containsPointInScissor(x, rowY + ROW_HEIGHT - 1))
            {
                extractRow(graphics, font, player, x, rowY, width, mouseX, mouseY);
            }

            rowY += ROW_HEIGHT;
        }
    }

    // Loading, no access, or an empty list; null when rows are shown.
    private Component message()
    {
        MachinePermissionsPayload payload = permissions.get();

        if (payload == null)
        {
            return text("loading");
        }

        if (payload.content().isEmpty())
        {
            return text("permissions.no_access");
        }

        return payload.content().get().trusted().isEmpty() ? text("trusted_players.empty") : null;
    }

    private List<TrustedPlayer> trusted()
    {
        return permissions.get().content().orElseThrow().trusted();
    }

    private static void extractRow(GuiGraphicsExtractor graphics, Font font, TrustedPlayer player,
                                   int x, int y, int width, int mouseX, int mouseY)
    {
        PlayerFaceExtractor.extractRenderState(graphics, skin(player.id()), x, y + (ROW_HEIGHT - FACE_SIZE) / 2 - 1,
                FACE_SIZE);

        int nameX = x + FACE_SIZE + FACE_GAP;
        Component name = player.name().<Component>map(Component::literal).orElse(text("trusted_players.unknown"));

        graphics.text(font, ScreenText.fitToWidth(font, name, x + width - nameX), nameX, y,
                player.name().isPresent() ? NAME_COLOR : UNKNOWN_COLOR, false);

        boolean hovered = mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + ROW_HEIGHT;

        if (player.name().isEmpty() && hovered && graphics.containsPointInScissor(mouseX, mouseY))
        {
            graphics.setTooltipForNextFrame(font, font.split(
                    Component.translatable("gui.fluxbound.trusted_players.unknown.help", player.id()), TOOLTIP_WIDTH),
                    mouseX, mouseY);
        }
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

    private static Component text(String key)
    {
        return Component.translatable("gui.fluxbound." + key);
    }
}

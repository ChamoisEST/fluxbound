package io.github.chamoisest.fluxbound.client.gui.tab;

import io.github.chamoisest.fluxbound.client.gui.OverlayHost;
import io.github.chamoisest.fluxbound.client.gui.widget.ConfirmationOverlay;
import io.github.chamoisest.fluxbound.client.gui.widget.FlatButton;
import io.github.chamoisest.fluxbound.client.gui.widget.NameField;
import io.github.chamoisest.fluxbound.machine.permission.MachineAccess;
import io.github.chamoisest.fluxbound.machine.permission.TrustChange;
import io.github.chamoisest.fluxbound.network.menu.MachinePermissionsPayload;
import io.github.chamoisest.fluxbound.network.menu.MachinePermissionsPayload.Content;
import io.github.chamoisest.fluxbound.network.menu.MachinePermissionsPayload.TrustedPlayer;
import io.github.chamoisest.fluxbound.network.menu.TrustResultPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

// A machine's trusted players, shared by all machine screens (M1 B6): a name field with an Add button, a status
// line for the server's answer (B6-5), and the list with a remove "×" per row (B6-6). Names come from the server
// (B4-3). The list always shows the synced state, so a change appears with the next sync.
public class TrustedPlayersTab implements MachineTab
{
    private static final int FIELD_HEIGHT = 14;
    private static final int ADD_WIDTH = 36;
    private static final int ADD_GAP = 4;
    private static final int STATUS_GAP = 3;
    private static final int LIST_GAP = 3;
    private static final int LINE_SPACING = 1;
    private static final int MESSAGE_COLOR = 0xFF555555;
    private static final int SUCCESS_COLOR = 0xFF2E7D32;
    private static final int ERROR_COLOR = 0xFFAA0000;

    private final Font font;
    private final Supplier<MachinePermissionsPayload> permissions;
    private final Supplier<TrustResultPayload> results;
    private final Supplier<@Nullable String> ownerName;
    private final OverlayHost host;
    private final Consumer<String> addRequest;
    private final Consumer<String> removeRequest;
    private final NameField nameField;
    private final FlatButton addButton;
    private final TrustedPlayerList list;
    private final List<AbstractWidget> widgets;
    private @Nullable TrustResultPayload lastResult;
    private @Nullable Component status;
    private int statusColor;

    // addRequest sends a typed name and removeRequest a stored ID for the open menu; the screen knows the
    // container and machine IDs. ownerName is the owner's display name from the screen's own sync, or null while
    // loading.
    public TrustedPlayersTab(Font font, Supplier<MachinePermissionsPayload> permissions,
                             Supplier<TrustResultPayload> results, Supplier<@Nullable String> ownerName,
                             OverlayHost host, Consumer<String> addRequest, Consumer<String> removeRequest)
    {
        this.font = font;
        this.permissions = permissions;
        this.results = results;
        this.ownerName = ownerName;
        this.host = host;
        this.addRequest = addRequest;
        this.removeRequest = removeRequest;
        this.lastResult = results.get();
        this.nameField = new NameField(font, FIELD_HEIGHT, text("trusted_players.name_hint"), this::suggestions,
                this::clearStatus, this::submit);
        this.addButton = new FlatButton(font, ADD_WIDTH, FIELD_HEIGHT, text("trusted_players.add"), this::submit);
        this.list = new TrustedPlayerList(font, this::trusted, this::remove);
        this.widgets = List.of(nameField, addButton, list);
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
    public List<AbstractWidget> widgets()
    {
        return widgets;
    }

    @Override
    public boolean widgetsShown()
    {
        return message() == null;
    }

    @Override
    public int contentHeight(Font font, int width)
    {
        Component message = message();

        if (message != null)
        {
            return textHeight(message, width);
        }

        int listHeight = trusted().isEmpty()
                ? textHeight(text("trusted_players.empty"), width)
                : trusted().size() * TrustedPlayerList.ROW_HEIGHT;

        return listOffset(width) + listHeight;
    }

    @Override
    public void extract(GuiGraphicsExtractor graphics, Font font, int x, int y, int width, int mouseX, int mouseY)
    {
        Component message = message();

        if (message != null)
        {
            extractLines(graphics, message, x, y, width, MESSAGE_COLOR);
            return;
        }

        pollResult();

        // The panel draws the widgets after this, at the positions set here.
        nameField.placeFrame(x, y, width - ADD_WIDTH - ADD_GAP);
        addButton.setX(x + width - ADD_WIDTH);
        addButton.setY(y);
        addButton.active = !nameField.getValue().isEmpty();

        if (status != null)
        {
            extractLines(graphics, status, x, y + FIELD_HEIGHT + STATUS_GAP, width, statusColor);
        }

        int listY = y + listOffset(width);
        list.place(x, listY, width);

        if (trusted().isEmpty())
        {
            extractLines(graphics, text("trusted_players.empty"), x, listY, width, MESSAGE_COLOR);
        }
    }

    // Enter in the field, or the Add button.
    private void submit()
    {
        String name = nameField.getValue();
        Optional<Content> content = content();

        if (name.isEmpty() || content.isEmpty())
        {
            return;
        }

        // Already listed under this name: answered here, without asking the server.
        Optional<String> listed = content.get().trusted().stream()
                .map(TrustedPlayer::name)
                .flatMap(Optional::stream)
                .filter(name::equalsIgnoreCase)
                .findFirst();

        if (listed.isPresent())
        {
            showResult(TrustChange.ALREADY_TRUSTED, listed.get());
            return;
        }

        setStatus(Component.translatable("gui.fluxbound.trusted_players.result.looking_up", name), MESSAGE_COLOR);
        addRequest.accept(name);
    }

    private void remove(TrustedPlayer player)
    {
        Optional<Content> content = content();

        if (content.isEmpty())
        {
            return;
        }

        if (removesOwnManage(content.get(), player))
        {
            host.openOverlay(new ConfirmationOverlay(font, text("permissions.confirm.remove_own_manage"),
                    () -> removeRequest.accept(player.id()), host));
            return;
        }

        removeRequest.accept(player.id());
    }

    // A non-owner who removes themselves loses everything set to Trusted, Manage included (M1 B5-4). The
    // prediction uses the server's own rule, but only decides whether to ask.
    private static boolean removesOwnManage(Content content, TrustedPlayer player)
    {
        String viewer = Minecraft.getInstance().player.getUUID().toString();

        return player.id().equals(viewer)
                && MachineAccess.grants(content.manage(), content.viewerIsOwner(), true)
                && !MachineAccess.grants(content.manage(), content.viewerIsOwner(), false);
    }

    // A new answer from the server replaces the status; a successful add also empties the field.
    private void pollResult()
    {
        TrustResultPayload result = results.get();

        if (result == null || result == lastResult)
        {
            return;
        }

        lastResult = result;

        // Cleared first, because editing the field clears the status.
        if (result.result() == TrustChange.ADDED && nameField.getValue().equalsIgnoreCase(result.name()))
        {
            nameField.setValue("");
        }

        showResult(result.result(), result.name());
    }

    private void showResult(TrustChange result, String name)
    {
        boolean success = result == TrustChange.ADDED || result == TrustChange.REMOVED;

        setStatus(Component.translatable(
                "gui.fluxbound.trusted_players.result." + result.name().toLowerCase(Locale.ROOT), name),
                success ? SUCCESS_COLOR : ERROR_COLOR);
    }

    private void setStatus(Component status, int color)
    {
        this.status = status;
        this.statusColor = color;
    }

    private void clearStatus()
    {
        status = null;
    }

    // Listed online players (the tab list, so players hidden by the server stay hidden) who aren't trusted yet,
    // without the viewer and the owner. The server would refuse the owner anyway.
    private List<String> suggestions()
    {
        Minecraft minecraft = Minecraft.getInstance();
        ClientPacketListener connection = minecraft.getConnection();

        if (connection == null || minecraft.player == null)
        {
            return List.of();
        }

        String own = minecraft.player.getGameProfile().name();
        String owner = ownerName.get();
        Set<String> trusted = trusted().stream()
                .map(TrustedPlayer::name)
                .flatMap(Optional::stream)
                .map(name -> name.toLowerCase(Locale.ROOT))
                .collect(Collectors.toSet());

        return connection.getListedOnlinePlayers().stream()
                .map(info -> info.getProfile().name())
                .filter(name -> !name.equalsIgnoreCase(own) && !name.equalsIgnoreCase(owner)
                        && !trusted.contains(name.toLowerCase(Locale.ROOT)))
                .toList();
    }

    // Loading until the first payload arrives, then a notice if the viewer lacks Manage; null when editing is shown.
    private @Nullable Component message()
    {
        MachinePermissionsPayload payload = permissions.get();

        if (payload == null)
        {
            return text("loading");
        }

        return payload.content().isEmpty() ? text("permissions.no_access") : null;
    }

    private Optional<Content> content()
    {
        MachinePermissionsPayload payload = permissions.get();
        return payload == null ? Optional.empty() : payload.content();
    }

    private List<TrustedPlayer> trusted()
    {
        return content().map(Content::trusted).orElse(List.of());
    }

    // Field, status line (one line always reserved, so the list doesn't jump), then the list.
    private int listOffset(int width)
    {
        int statusHeight = status == null ? font.lineHeight + LINE_SPACING : textHeight(status, width);
        return FIELD_HEIGHT + STATUS_GAP + statusHeight + LIST_GAP;
    }

    private int textHeight(Component text, int width)
    {
        return font.split(text, width).size() * (font.lineHeight + LINE_SPACING);
    }

    private void extractLines(GuiGraphicsExtractor graphics, Component text, int x, int y, int width, int color)
    {
        int lineY = y;

        for (FormattedCharSequence line : font.split(text, width))
        {
            graphics.text(font, line, x, lineY, color, false);
            lineY += font.lineHeight + LINE_SPACING;
        }
    }

    private static Component text(String key)
    {
        return Component.translatable("gui.fluxbound." + key);
    }
}

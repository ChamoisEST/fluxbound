package io.github.chamoisest.fluxbound.client.gui.tab;

import io.github.chamoisest.fluxbound.client.gui.OverlayHost;
import io.github.chamoisest.fluxbound.client.gui.ScreenText;
import io.github.chamoisest.fluxbound.client.gui.widget.ConfirmationOverlay;
import io.github.chamoisest.fluxbound.client.gui.widget.DropdownWidget;
import io.github.chamoisest.fluxbound.machine.permission.AccessMode;
import io.github.chamoisest.fluxbound.machine.permission.MachineAccess;
import io.github.chamoisest.fluxbound.machine.permission.MachineRight;
import io.github.chamoisest.fluxbound.machine.permission.PermissionNames;
import io.github.chamoisest.fluxbound.network.menu.MachinePermissionsPayload;
import io.github.chamoisest.fluxbound.network.menu.MachinePermissionsPayload.Content;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.FormattedCharSequence;
import org.jspecify.annotations.Nullable;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

// Lists a machine's access rights, one line per right, with a dropdown to change each (shared by all machine
// screens). Each screen passes the rights its machine uses, described in its own words; the descriptions appear
// in the "?" tooltip (M1 B4b-1). The data arrives only for viewers with Manage (M1 B4-1). A choice only sends a
// request; the dropdown shows the new value once the server's sync confirms it (M1 B5-1).
public class PermissionsTab implements MachineTab
{
    private static final int LINE_SPACING = 1;
    private static final int ROW_HEIGHT = 14;
    private static final int ROW_GAP = 2;
    private static final int DROPDOWN_WIDTH = 80;
    private static final int NAME_GAP = 6;
    private static final int NAME_COLOR = 0xFF404040;
    private static final int MESSAGE_COLOR = 0xFF555555;

    // Team stays out of the choices until it works (M1 B5-2); a stored Team value is still shown as current.
    private static final List<AccessMode> CHOICES =
            Arrays.stream(AccessMode.values()).filter(AccessMode::selectable).toList();
    private static final Tooltip TEAM_TOOLTIP = Tooltip.create(text("access_mode.team.help"));

    // One right shown by this screen, with a description that fits the machine.
    public record Row(MachineRight right, Component description)
    {
    }

    private final Font font;
    private final Supplier<MachinePermissionsPayload> permissions;
    private final List<Row> rows;
    private final Component help;
    private final OverlayHost host;
    private final BiConsumer<MachineRight, AccessMode> request;
    private final List<DropdownWidget<AccessMode>> dropdowns;

    // request sends a change for the open menu; the screen knows the container and machine IDs.
    public PermissionsTab(Font font, Supplier<MachinePermissionsPayload> permissions, List<Row> rows,
                          OverlayHost host, BiConsumer<MachineRight, AccessMode> request)
    {
        this.font = font;
        this.permissions = permissions;
        this.rows = List.copyOf(rows);
        this.help = buildHelp(this.rows);
        this.host = host;
        this.request = request;
        this.dropdowns = this.rows.stream()
                .map(row -> new DropdownWidget<>(font, DROPDOWN_WIDTH, ROW_HEIGHT, CHOICES, PermissionNames::mode,
                        () -> currentMode(row.right()), mode -> choose(row.right(), mode), host))
                .toList();
    }

    @Override
    public MachineTabType type()
    {
        return MachineTabType.PERMISSIONS;
    }

    @Override
    public Component help()
    {
        return help;
    }

    @Override
    public List<DropdownWidget<AccessMode>> widgets()
    {
        return dropdowns;
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
            return font.split(message, width).size() * (font.lineHeight + LINE_SPACING);
        }

        return Math.max(0, rows.size() * (ROW_HEIGHT + ROW_GAP) - ROW_GAP);
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

        Content content = permissions.get().content().orElseThrow();
        int rowY = y;

        for (int index = 0; index < rows.size(); index++)
        {
            MachineRight right = rows.get(index).right();
            DropdownWidget<AccessMode> dropdown = dropdowns.get(index);
            int dropdownX = x + width - DROPDOWN_WIDTH;

            graphics.text(font, ScreenText.fitToWidth(font, PermissionNames.right(right), dropdownX - NAME_GAP - x),
                    x, rowY + (ROW_HEIGHT - font.lineHeight) / 2 + 1, NAME_COLOR, false);

            // The panel draws the dropdown after this, at the position set here.
            dropdown.setX(dropdownX);
            dropdown.setY(rowY);

            // Team is stored but not functional yet, so the field explains itself on hover (M1 B4-5).
            dropdown.setTooltip(content.mode(right) == AccessMode.TEAM ? TEAM_TOOLTIP : null);

            rowY += ROW_HEIGHT + ROW_GAP;
        }
    }

    // Loading until the first payload arrives, then a notice if the viewer lacks Manage; null when rows are shown.
    private @Nullable Component message()
    {
        MachinePermissionsPayload payload = permissions.get();

        if (payload == null)
        {
            return text("loading");
        }

        return payload.content().isEmpty() ? text("permissions.no_access") : null;
    }

    // Only called while rows are shown, so the content exists.
    private AccessMode currentMode(MachineRight right)
    {
        return permissions.get().content().orElseThrow().mode(right);
    }

    private void choose(MachineRight right, AccessMode mode)
    {
        MachinePermissionsPayload payload = permissions.get();
        Optional<Content> content = payload == null ? Optional.empty() : payload.content();

        if (content.isEmpty())
        {
            return;
        }

        Component warning = warningFor(content.get(), right, mode);

        if (warning == null)
        {
            request.accept(right, mode);
            return;
        }

        host.openOverlay(new ConfirmationOverlay(font, warning, () -> request.accept(right, mode), host));
    }

    // Manage = Everyone gives every player full control (M1 E3); a non-owner who removes their own Manage can't
    // undo it (M1 B5-4). The prediction uses the server's own rule, but only decides whether to ask.
    private static @Nullable Component warningFor(Content content, MachineRight right, AccessMode mode)
    {
        if (right != MachineRight.MANAGE)
        {
            return null;
        }

        if (mode == AccessMode.EVERYONE)
        {
            return text("permissions.confirm.manage_everyone");
        }

        boolean trusted = viewerIsTrusted(content);
        boolean losesManage = MachineAccess.grants(content.manage(), content.viewerIsOwner(), trusted)
                && !MachineAccess.grants(mode, content.viewerIsOwner(), trusted);

        return losesManage ? text("permissions.confirm.remove_own_manage") : null;
    }

    private static boolean viewerIsTrusted(Content content)
    {
        String viewer = Minecraft.getInstance().player.getUUID().toString();
        return content.trusted().stream().anyMatch(player -> player.id().equals(viewer));
    }

    // The general explanation, then "Right: description" for each shown right, one per line.
    private static Component buildHelp(List<Row> rows)
    {
        MutableComponent help = Component.empty().append(text("permissions.help"));

        for (Row row : rows)
        {
            help.append("\n").append(PermissionNames.right(row.right())).append(": ").append(row.description());
        }

        return help;
    }

    private static Component text(String key)
    {
        return Component.translatable("gui.fluxbound." + key);
    }
}


package io.github.chamoisest.fluxbound.command;

import io.github.chamoisest.fluxbound.Fluxbound;
import io.github.chamoisest.fluxbound.machine.data.MachineRecord;
import io.github.chamoisest.fluxbound.machine.data.MachineRegistryData;
import io.github.chamoisest.fluxbound.machine.permission.AccessMode;
import io.github.chamoisest.fluxbound.machine.permission.MachineAccess;
import io.github.chamoisest.fluxbound.machine.permission.MachineRight;
import io.github.chamoisest.fluxbound.machine.permission.MachineRights;
import io.github.chamoisest.fluxbound.machine.permission.PermissionNames;
import io.github.chamoisest.fluxbound.machine.permission.PermissionChange;
import io.github.chamoisest.fluxbound.machine.permission.TrustChange;
import io.github.chamoisest.fluxbound.machine.permission.TrustRequests;
import io.github.chamoisest.fluxbound.menu.PlayerNames;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;

import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@EventBusSubscriber(modid = Fluxbound.MODID)
public final class ModCommands
{
    private ModCommands() {}

    @SubscribeEvent
    public static void register(RegisterCommandsEvent event)
    {
        var root = Commands.literal("fluxbound");
        var access = Commands.literal("access");

        access.then(Commands.literal("show").executes(context -> show(context.getSource())));

        for (MachineRight right : MachineRight.values()) {
            var rightNode = Commands.literal(right.name().toLowerCase(Locale.ROOT));

            for (AccessMode mode : AccessMode.values()) {
                if (!mode.selectable())
                {
                    continue;
                }

                rightNode.then(Commands.literal(mode.name().toLowerCase(Locale.ROOT))
                        .executes(context -> setMode(context.getSource(), right, mode)));
            }

            access.then(rightNode);
        }

        root.then(access);

        // Plain names rather than entity arguments, so offline players can be added and removed too (M1 B6-7).
        root.then(Commands.literal("trust")
                .then(Commands.literal("add")
                        .then(Commands.argument("player", StringArgumentType.word())
                                .suggests((context, builder) -> SharedSuggestionProvider.suggest(
                                        context.getSource().getOnlinePlayerNames(), builder))
                                .executes(context -> addTrusted(context.getSource(),
                                        StringArgumentType.getString(context, "player")))))
                .then(Commands.literal("remove")
                        .then(Commands.argument("player", StringArgumentType.word())
                                .suggests(ModCommands::suggestTrusted)
                                .executes(context -> removeTrusted(context.getSource(),
                                        StringArgumentType.getString(context, "player"))))));

        event.getDispatcher().register(root);
    }

    private static MachineRecord lookedAt(ServerPlayer player)
    {
        ServerLevel level = player.level();
        HitResult hit = player.pick(6.0, 0.0F, false);

        if (!(hit instanceof BlockHitResult blockHit) || hit.getType() != HitResult.Type.BLOCK) {
            return null;
        }

        return MachineRegistryData.get(level).findAt(
                level.dimension().identifier().toString(), blockHit.getBlockPos().asLong());
    }

    private static int show(CommandSourceStack source) throws CommandSyntaxException
    {
        ServerPlayer player = source.getPlayerOrException();
        MachineRecord machine = lookedAt(player);

        if (!MachineAccess.has(player, machine, MachineRight.MANAGE))
        {
            source.sendFailure(Component.translatable("commands.fluxbound.denied"));
            return 0;
        }

        for (MachineRight right : MachineRights.of(machine))
        {
            Component line = Component.translatable("commands.fluxbound.access.show.mode",
                    PermissionNames.right(right), PermissionNames.mode(machine.permissions().mode(right)));
            source.sendSuccess(() -> line, false);
        }

        source.sendSuccess(() -> Component.translatable("commands.fluxbound.access.show.trusted",
                machine.permissions().trusted().size()), false);

        return 1;
    }

    private static int setMode(CommandSourceStack source, MachineRight right, AccessMode mode)
            throws CommandSyntaxException
    {
        ServerPlayer player = source.getPlayerOrException();
        MachineRecord machine = lookedAt(player);

        PermissionChange result = machine == null
                ? PermissionChange.DENIED
                : MachineRegistryData.get(source.getLevel()).setAccessMode(player, machine.id(), right, mode);

        switch (result)
        {
            case DENIED ->
            {
                source.sendFailure(Component.translatable("commands.fluxbound.denied"));
                return 0;
            }
            case RIGHT_NOT_USED ->
            {
                source.sendFailure(Component.translatable("commands.fluxbound.access.right_not_used",
                        PermissionNames.right(right)));
                return 0;
            }
            case MODE_UNAVAILABLE ->
            {
                source.sendFailure(Component.translatable("commands.fluxbound.access.mode_unavailable",
                        PermissionNames.mode(mode)));
                return 0;
            }
            case APPLIED ->
            {
                source.sendSuccess(() -> Component.translatable("commands.fluxbound.access.set",
                        PermissionNames.right(right), PermissionNames.mode(mode)), false);

                if (right == MachineRight.MANAGE && mode == AccessMode.EVERYONE)
                {
                    source.sendSuccess(() -> Component.translatable("commands.fluxbound.access.everyone_warning")
                            .withStyle(ChatFormatting.YELLOW), false);
                }

                return 1;
            }
        }

        return 0;
    }

    // The machine is the one looked at when the command runs, even if the name lookup finishes later.
    private static int addTrusted(CommandSourceStack source, String name) throws CommandSyntaxException
    {
        ServerPlayer player = source.getPlayerOrException();
        MachineRecord machine = lookedAt(player);

        if (machine == null)
        {
            source.sendFailure(Component.translatable("commands.fluxbound.denied"));
            return 0;
        }

        // The answer can arrive after the command has finished, so it goes to chat directly.
        TrustRequests.add(player, machine.id(), name, requester -> true, ModCommands::sendTrustFeedback);
        return 1;
    }

    // Matched against the machine's own list by stored ID or cached name, so no lookup is needed and players who
    // renamed but haven't rejoined are still found under the name the list shows (M1 B6a-2).
    private static int removeTrusted(CommandSourceStack source, String name) throws CommandSyntaxException
    {
        ServerPlayer player = source.getPlayerOrException();
        MachineRecord machine = lookedAt(player);

        if (!MachineAccess.has(player, machine, MachineRight.MANAGE))
        {
            source.sendFailure(Component.translatable("commands.fluxbound.denied"));
            return 0;
        }

        MinecraftServer server = source.getServer();
        Optional<String> storedId = machine.permissions().trusted().stream()
                .filter(id -> id.equals(name) || PlayerNames.find(server, id).filter(name::equalsIgnoreCase).isPresent())
                .findFirst();

        if (storedId.isEmpty())
        {
            sendTrustFeedback(player, TrustChange.NOT_TRUSTED, name);
            return 0;
        }

        TrustChange result = MachineRegistryData.get(source.getLevel()).removeTrusted(player, machine.id(), storedId.get());
        sendTrustFeedback(player, result, PlayerNames.find(server, storedId.get()).orElse(storedId.get()));
        return result == TrustChange.REMOVED ? 1 : 0;
    }

    // Only players with Manage on the looked-at machine see its trusted names.
    private static CompletableFuture<Suggestions> suggestTrusted(CommandContext<CommandSourceStack> context,
                                                                 SuggestionsBuilder builder)
    {
        if (!(context.getSource().getEntity() instanceof ServerPlayer player))
        {
            return builder.buildFuture();
        }

        MachineRecord machine = lookedAt(player);

        if (!MachineAccess.has(player, machine, MachineRight.MANAGE))
        {
            return builder.buildFuture();
        }

        MinecraftServer server = context.getSource().getServer();

        return SharedSuggestionProvider.suggest(machine.permissions().trusted().stream()
                .map(id -> PlayerNames.find(server, id).orElse(id)), builder);
    }

    private static void sendTrustFeedback(ServerPlayer player, TrustChange result, String name)
    {
        Component message = switch (result)
        {
            case ADDED -> Component.translatable("commands.fluxbound.trust.added", name);
            case REMOVED -> Component.translatable("commands.fluxbound.trust.removed", name);
            case DENIED -> Component.translatable("commands.fluxbound.denied");
            case ALREADY_TRUSTED -> Component.translatable("commands.fluxbound.trust.already_trusted", name);
            case NOT_TRUSTED -> Component.translatable("commands.fluxbound.trust.not_trusted", name);
            case IS_OWNER -> Component.translatable("commands.fluxbound.trust.is_owner", name);
            case INVALID_NAME -> Component.translatable("commands.fluxbound.trust.invalid_name", name);
            case NOT_FOUND -> Component.translatable("commands.fluxbound.trust.not_found", name);
            case LOOKUP_FAILED -> Component.translatable("commands.fluxbound.trust.lookup_failed", name);
            case LOOKUP_BUSY -> Component.translatable("commands.fluxbound.trust.lookup_busy");
        };

        boolean success = result == TrustChange.ADDED || result == TrustChange.REMOVED;
        player.sendSystemMessage(success ? message : message.copy().withStyle(ChatFormatting.RED));
    }
}

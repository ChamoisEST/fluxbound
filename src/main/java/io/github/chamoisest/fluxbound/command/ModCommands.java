
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
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

import com.mojang.brigadier.exceptions.CommandSyntaxException;

import java.util.Locale;

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

        root.then(Commands.literal("trust")
                .then(Commands.literal("add")
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(context -> setTrusted(context.getSource(),
                                        EntityArgument.getPlayer(context, "player"), true))))
                .then(Commands.literal("remove")
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(context -> setTrusted(context.getSource(),
                                        EntityArgument.getPlayer(context, "player"), false)))));

        event.getDispatcher().register(root);
    }

    private static MachineRecord lookedAt(CommandSourceStack source) throws CommandSyntaxException
    {
        ServerPlayer player = source.getPlayerOrException();
        ServerLevel level = source.getLevel();
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
        MachineRecord machine = lookedAt(source);

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
        MachineRecord machine = lookedAt(source);

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

    private static int setTrusted(CommandSourceStack source, ServerPlayer target, boolean enabled)
            throws CommandSyntaxException
    {
        ServerPlayer player = source.getPlayerOrException();
        MachineRecord machine = lookedAt(source);

        if (machine == null || !MachineRegistryData.get(source.getLevel())
                .setTrusted(player, machine.id(), target.getUUID(), enabled))
        {
            source.sendFailure(Component.translatable("commands.fluxbound.denied"));
            return 0;
        }

        String key = enabled ? "commands.fluxbound.trust.added" : "commands.fluxbound.trust.removed";
        source.sendSuccess(() -> Component.translatable(key, target.getName()), false);
        return 1;
    }
}

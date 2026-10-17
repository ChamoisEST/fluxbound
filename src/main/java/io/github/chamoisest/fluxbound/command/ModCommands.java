
package io.github.chamoisest.fluxbound.command;

import io.github.chamoisest.fluxbound.Fluxbound;
import io.github.chamoisest.fluxbound.machine.data.MachineRecord;
import io.github.chamoisest.fluxbound.machine.data.MachineRegistryData;
import io.github.chamoisest.fluxbound.machine.permission.AccessMode;
import io.github.chamoisest.fluxbound.machine.permission.MachineAccess;
import io.github.chamoisest.fluxbound.machine.permission.MachineRight;
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

        if (!MachineAccess.has(player, machine, MachineRight.MANAGE)) {
            source.sendFailure(Component.literal("No machine selected, or Manage permission denied."));
            return 0;
        }

        for (MachineRight right : MachineRight.values()) {
            String text = right + ": " + machine.permissions().mode(right);
            source.sendSuccess(() -> Component.literal(text), false);
        }

        source.sendSuccess(() -> Component.literal(
                "Trusted players: " + machine.permissions().trusted().size()), false);

        return 1;
    }

    private static int setMode(CommandSourceStack source, MachineRight right, AccessMode mode)
            throws CommandSyntaxException
    {
        ServerPlayer player = source.getPlayerOrException();
        MachineRecord machine = lookedAt(source);

        if (machine == null || !MachineRegistryData.get(source.getLevel())
                .setAccessMode(player, machine.id(), right, mode)) {
            source.sendFailure(Component.literal("No machine selected, or Manage permission denied."));
            return 0;
        }

        source.sendSuccess(() -> Component.literal(right + " set to " + mode), false);
        return 1;
    }

    private static int setTrusted(CommandSourceStack source, ServerPlayer target, boolean enabled)
            throws CommandSyntaxException
    {
        ServerPlayer player = source.getPlayerOrException();
        MachineRecord machine = lookedAt(source);

        if (machine == null || !MachineRegistryData.get(source.getLevel())
                .setTrusted(player, machine.id(), target.getUUID(), enabled)) {
            source.sendFailure(Component.literal("No machine selected, or Manage permission denied."));
            return 0;
        }

        String action = enabled ? "Trusted" : "Untrusted";
        source.sendSuccess(() -> Component.literal(action + " " + target.getName().getString()), false);
        return 1;
    }
}

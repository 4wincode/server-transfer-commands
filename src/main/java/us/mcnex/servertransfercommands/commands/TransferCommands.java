package us.mcnex.servertransfercommands.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.ClientboundTransferPacket;
import net.minecraft.server.level.ServerPlayer;
import us.mcnex.servertransfercommands.ServerTransferCommands;
import us.mcnex.servertransfercommands.config.Config;

import java.util.Collection;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

public class TransferCommands {
    private static final SimpleCommandExceptionType ERROR_MUST_BE_PLAYER = new SimpleCommandExceptionType(Component.literal("You must be a player to run this command!"));
    private static final SimpleCommandExceptionType ERROR_ALREADY_CONNECTED = new SimpleCommandExceptionType(Component.literal("You are already connected to this server!"));
    private static final SimpleCommandExceptionType ERROR_TARGET_ALREADY_CONNECTED = new SimpleCommandExceptionType(Component.literal("The target is already connected to this server!"));

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        Config config = ServerTransferCommands.getConfig();

        if (!config.getDisabledCommands().contains("server")) {
            var serverCommand = literal("server").requires(Permissions.require("server-transfer-commands.command.server"));
            for (var server : config.getServers().values()) {
                var subcommand = literal(server.name());
                if (server.restricted()) {
                    subcommand.requires(Permissions.require("server-transfer-commands.server." + server.name()));
                }
                subcommand.executes(ctx -> {
                    if (config.getThisServer() == server) {
                        throw ERROR_ALREADY_CONNECTED.create();
                    } else {
                        if (!ctx.getSource().isPlayer())
                            throw ERROR_MUST_BE_PLAYER.create();
                        transfer(ctx.getSource().getPlayerOrException(), server.hostName(), server.port());
                        return 1;
                    }
                });
                serverCommand.then(subcommand);
            }
            dispatcher.register(serverCommand);
        }

        if (!config.getDisabledCommands().contains("send")) {
            var sendCommand = literal("send").requires(Permissions.require("server-transfer-commands.command.send"));
            var argument = argument("players", EntityArgument.players());
            for (var server : config.getServers().values()) {
                var subcommand = literal(server.name());
                subcommand.executes(ctx -> {
                    if (config.getThisServer() == server) {
                        throw ERROR_TARGET_ALREADY_CONNECTED.create();
                    } else {
                        var players = EntityArgument.getPlayers(ctx, "players");
                        send(ctx.getSource(), players, server.hostName(), server.port());
                        return 1;
                    }
                });
                argument.then(subcommand);
            }
            sendCommand.then(argument);
            dispatcher.register(sendCommand);
        }

        if (!config.getDisabledCommands().contains("send-ip")) {
            dispatcher.register(literal("send-ip")
                    .requires(Permissions.require("server-transfer-commands.command.send-ip"))
                    .then(argument("players", EntityArgument.players())
                            .then(argument("hostname", StringArgumentType.string())
                                    .executes(ctx -> {
                                        send(ctx, false);
                                        return 1;
                                    })
                                    .then(argument("port", IntegerArgumentType.integer(1, 65535))
                                            .executes(ctx -> {
                                                send(ctx, true);
                                                return 1;
                                            })))));
        }
    }

    private static void send(CommandContext<CommandSourceStack> ctx, boolean specifiesPort) throws CommandSyntaxException {
        var players = EntityArgument.getPlayers(ctx, "players");
        String hostName = StringArgumentType.getString(ctx, "hostname");
        int port = specifiesPort ? IntegerArgumentType.getInteger(ctx, "port") : 25565;
        send(ctx.getSource(), players, hostName, port);
    }

    private static void send(CommandSourceStack source, Collection<ServerPlayer> players, String hostName, int port) {
        int playerCount = players.size();
        transfer(players, hostName, port);
        source.sendSystemMessage(Component.literal("Attempting to send " + playerCount + " player" + (playerCount > 1 ? "s" : "") + " to " + hostName + ":" + port).withStyle(ChatFormatting.DARK_GREEN));
    }

    private static void transfer(ServerPlayer player, String hostName, int port) {
        var transferPacket = new ClientboundTransferPacket(hostName, port);
        player.connection.send(transferPacket);
    }

    private static void transfer(Collection<ServerPlayer> players, String hostName, int port) {
        for (ServerPlayer player : players)
            transfer(player, hostName, port);
    }
}
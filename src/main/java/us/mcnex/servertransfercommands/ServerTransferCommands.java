package us.mcnex.servertransfercommands;

import lombok.Getter;
import me.lucko.fabric.api.permissions.v0.PermissionCheckEvent;
import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.util.TriState;
import us.mcnex.servertransfercommands.commands.TransferCommands;
import us.mcnex.servertransfercommands.config.Config;

public class ServerTransferCommands implements DedicatedServerModInitializer {
    @Getter
    private static Config config;

    @Override
    public void onInitializeServer() {
        config = new Config("server-transfer-commands.yml");

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> TransferCommands.register(dispatcher));

        PermissionCheckEvent.EVENT.register((source, permission) -> {
            if (config.hasPermission(source, permission)) {
                return TriState.TRUE;
            }
            return TriState.DEFAULT;
        });
    }
}

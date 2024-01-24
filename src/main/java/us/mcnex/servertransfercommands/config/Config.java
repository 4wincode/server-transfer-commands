package us.mcnex.servertransfercommands.config;

import com.mojang.logging.LogUtils;
import lombok.Getter;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.SharedSuggestionProvider;
import org.slf4j.Logger;
import org.yaml.snakeyaml.Yaml;

import java.io.FileReader;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

@Getter
public class Config {
    private static final Logger LOGGER = LogUtils.getLogger();

    private final ServerDefinition thisServer;
    private final Map<String, List<String>> permissions;
    private final List<String> disabledCommands;
    private final Map<String, ServerDefinition> servers;
    private final Map<String, List<String>> groups;
    private final Map<Integer, List<String>> levelGroups;

    @SuppressWarnings("unchecked")
    public Config(String configName) {
        Map<String, Object> map;
        try {
            Path configPath = FabricLoader.getInstance().getConfigDir().resolve(configName);
            if (!Files.exists(configPath)) {
                FabricLoader.getInstance().getModContainer("server-transfer-commands").ifPresent(modContainer -> {
                    Path path = modContainer.findPath("config.yml").orElseThrow();
                    try {
                        Files.copy(path, configPath);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });
            }

            Yaml yaml = new Yaml();
            map = yaml.load(new FileReader(configPath.toFile()));

            permissions = (Map<String, List<String>>) map.getOrDefault("permissions", Collections.emptyMap());
            disabledCommands = (List<String>) map.getOrDefault("disabled_commands", Collections.emptySet());
            groups = (Map<String, List<String>>) map.getOrDefault("groups", Collections.emptyMap());
            levelGroups = (Map<Integer, List<String>>) map.getOrDefault("level_groups", Collections.emptyMap());
            servers = new HashMap<>();

            if (map.containsKey("servers")) {
                for (var server : ((Map<String, Map<String, Object>>) map.get("servers")).entrySet()) {
                    URI address;
                    try {
                        address = new URI("tcp://" + server.getValue().getOrDefault("address", "localhost:25565"));
                    } catch (URISyntaxException e) {
                        LOGGER.warn("Invalid address for server " + server.getKey() + ", skipping");
                        continue;
                    }
                    String hostName = address.getHost();
                    int port = address.getPort() != -1 ? address.getPort() : 25565;

                    boolean restricted = (boolean) server.getValue().getOrDefault("restricted", false);
                    servers.put(server.getKey(), new ServerDefinition(server.getKey(), hostName, port, restricted));
                }
            }

            thisServer = servers.get((String) map.get("this_server"));
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    public List<String> getGroups(String player) {
        var playerGroups = groups.getOrDefault(player, new ArrayList<>());
        if (!playerGroups.contains("default"))
            playerGroups.add(0, "default");
        return playerGroups;
    }

    public Boolean hasPermission(String player, String permission) {
        for (String group : getGroups(player)) {
            if (permissions.get(group).contains(permission))
                return true;
        }
        return false;
    }

    public Boolean hasPermission(SharedSuggestionProvider source, String permission) {
        for (var levelGroupEntry : levelGroups.entrySet()) {
            if (source.hasPermission(levelGroupEntry.getKey())) {
                for (String group : levelGroupEntry.getValue()) {
                    if (permissions.get(group).contains(permission))
                        return true;
                }
            }
        }
        if (source instanceof CommandSourceStack commandSourceStack && commandSourceStack.isPlayer())
            return hasPermission(Objects.requireNonNull(commandSourceStack.getPlayer()).getName().getString(), permission);
        return false;
    }
}

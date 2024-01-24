# Server Transfer Commands

This mod adds commands to transfer yourself or another player to a different server using the new transfer packet added in 24w03a. The commands and config are loosely based on BungeeCord, so they should be familiar if you've used it or a similar proxy before.

This mod does not expose an API for transferring players or setting/getting cookies; it just sends the packet. This is something that may be added in the near future, though.

## Commands

Currently, the following commands are implemented:

- `/server <server>`: Go to a configured server that isn't restricted or you have permission to access.
- `/send <players> <server>`: Sends one or more players to a configured server. Restrictions are not considered.
- `/send-ip <players> <hostname> [<port>]`: Sends one or more players to the specified hostname and port. If no port is specified, it defaults to 25565.

Commands that would query the servers like `/glist` and `/find` have not been added yet but are in progress.

## Permissions

Permissions can be set with groups in the config file or with any mod that supports the [Fabric Permissions API](https://github.com/lucko/fabric-permissions-api). If you would like to disable a command entirely, you can add it to the `disabled_commands` list in the config.

- `server-transfer-commands.command.server`
- `server-transfer-commands.command.send`
- `server-transfer-commands.command.send-ip`
- `server-transfer-commands.server.<server>`: Permission to use `/server` to go to a server that has `restricted` enabled. `/send` and `/send-ip` do not consider this.

## Config

The commented default config is as follows:

```yaml
# The list of servers that can be accessed with /server or /send.
# If "restricted" is true, the "server-transfer-commands.server.<server>" permission is required to access it with /server.
servers:
  lobby:
    address: localhost:25565
    restricted: false
  survival:
    address: localhost:25564
    restricted: false

# A server that should show "You are already connected to this server!" when you attempt to transfer to it.
this_server: lobby

# Commands that should not be registered.
disabled_commands:
  - disabledcommandhere

# Permission groups for assigning to players or permission levels.
permissions:
  default:
    - server-transfer-commands.command.server
  admin:
    - server-transfer-commands.command.send
    - server-transfer-commands.command.send-ip

# Permission groups assigned to specific players.
groups:
  4win:
    - admin

# Permission groups assigned to Minecraft permission levels, normally from 1 to 4.
# Level 3 is what allows use of multiplayer management commands such as /kick.
level_groups:
  3:
    - admin
```
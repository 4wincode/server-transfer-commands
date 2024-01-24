package us.mcnex.servertransfercommands.config;

public record ServerDefinition (
    String name,
    String hostName,
    int port,
    boolean restricted
) {}
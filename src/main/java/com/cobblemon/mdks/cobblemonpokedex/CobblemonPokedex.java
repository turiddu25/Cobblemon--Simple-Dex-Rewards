package com.cobblemon.mdks.cobblemonpokedex;

import com.cobblemon.mdks.cobblemonpokedex.config.Config;
import com.cobblemon.mdks.cobblemonpokedex.config.PlayerDataConfig;
import com.cobblemon.mdks.cobblemonpokedex.command.DexRewardsCommand;
import com.cobblemon.mdks.cobblemonpokedex.util.*;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.server.MinecraftServer;
import org.slf4j.LoggerFactory;

public class CobblemonPokedex implements ModInitializer {
    public static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger("cobblemonpokedex");
    public static Config config;
    public static PlayerDataConfig playerDataConfig;
    public static Permissions permissions;
    public static MinecraftServer server;

    @Override
    public void onInitialize() {
        // Load configs
        config = new Config();
        config.load();
        
        playerDataConfig = new PlayerDataConfig();

        // Initialize permissions
        permissions = new Permissions();

        // Add commands to registry
        CommandsRegistry.addCommand(new DexRewardsCommand());

        // Register commands with Fabric
        CommandRegistrationCallback.EVENT.register(CommandsRegistry::registerCommands);

        LOGGER.info("Cobblemon Pokedex Progression initialized");

        // Register event listeners
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            CobblemonPokedex.server = server;
            // TODO: Register Pokedex event listeners
        });
    }

    public static void reload() {
        config.load();
        playerDataConfig = new PlayerDataConfig();
        permissions = new Permissions();
        LOGGER.info("Cobblemon Pokedex Progression reloaded");
    }
}

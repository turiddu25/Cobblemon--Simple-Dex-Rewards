package com.cobblemon.mdks.cobblemonpokedex;

import com.cobblemon.mdks.cobblemonpokedex.config.PlayerDataConfig;
import com.cobblemon.mdks.cobblemonpokedex.config.PokedexConfig;
import com.cobblemon.mdks.cobblemonpokedex.config.RewardConfig;
import com.cobblemon.mdks.cobblemonpokedex.command.DexRewardsCommand;
import com.cobblemon.mdks.cobblemonpokedex.util.*;
import com.cobblemon.mdks.cobblemonpokedex.listeners.CatchPokemonListener;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.server.MinecraftServer;
import org.slf4j.LoggerFactory;

public class CobblemonPokedex implements ModInitializer {
    public static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger("cobblemonpokedex");
    public static PokedexConfig pokedexConfig;
    public static RewardConfig rewardConfig;
    public static PlayerDataConfig playerDataConfig;
    public static Permissions permissions;
    public static MinecraftServer server;

    @Override
    public void onInitialize() {
        try {
            // Create base config directory
            Utils.checkForDirectory("/config/cobblemonpokedex");
            
            // Load configs in order
            LOGGER.info("Loading Pokedex configuration...");
            pokedexConfig = new PokedexConfig();
            pokedexConfig.load();
            
            LOGGER.info("Loading reward configuration...");
            rewardConfig = new RewardConfig();
            rewardConfig.load();
            
            LOGGER.info("Loading player data configuration...");
            playerDataConfig = new PlayerDataConfig();
            
            LOGGER.info("All configurations loaded successfully");
            
            LOGGER.info("All configurations loaded successfully");
        } catch (Exception e) {
            LOGGER.error("Failed to load configurations", e);
            throw new RuntimeException("Failed to initialize Cobblemon Pokedex", e);
        }

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
            CatchPokemonListener.register();
            LOGGER.info("Registered Pokemon catch listener");
        });
    }

    public static void reload() {
        try {
            LOGGER.info("Reloading configurations...");
            pokedexConfig.load();
            rewardConfig.load();
            playerDataConfig = new PlayerDataConfig();
            permissions = new Permissions();
            LOGGER.info("Cobblemon Pokedex Progression reloaded successfully");
        } catch (Exception e) {
            LOGGER.error("Failed to reload configurations", e);
            throw new RuntimeException("Failed to reload Cobblemon Pokedex", e);
        }
    }
}

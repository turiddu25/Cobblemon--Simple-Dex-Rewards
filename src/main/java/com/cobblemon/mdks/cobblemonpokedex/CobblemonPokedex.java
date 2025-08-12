package com.cobblemon.mdks.cobblemonpokedex;

import org.slf4j.LoggerFactory;

import com.cobblemon.mdks.cobblemonpokedex.command.DexRewardsCommand;
import com.cobblemon.mdks.cobblemonpokedex.config.PlayerDataConfig;
import com.cobblemon.mdks.cobblemonpokedex.config.PokedexConfig;
import com.cobblemon.mdks.cobblemonpokedex.config.RewardConfig;
import com.cobblemon.mdks.cobblemonpokedex.listeners.CatchPokemonListener;
import com.cobblemon.mdks.cobblemonpokedex.util.CommandsRegistry;
import com.cobblemon.mdks.cobblemonpokedex.util.MessageHandler;
import com.cobblemon.mdks.cobblemonpokedex.util.Permissions;
import com.cobblemon.mdks.cobblemonpokedex.util.Utils;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.server.MinecraftServer;

public class CobblemonPokedex implements ModInitializer {
    public static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger("simpledexrewards");
    public static PokedexConfig pokedexConfig;
    public static RewardConfig rewardConfig;
    public static PlayerDataConfig playerDataConfig;
    public static Permissions permissions;
    public static MinecraftServer server;

    @Override
    public void onInitialize() {
        try {
            // Create base config directory
            Utils.checkForDirectory("/config/simpledexrewards");
            
            // Load configs in order
            LOGGER.info("Loading Pokedex configuration...");
            pokedexConfig = new PokedexConfig();
            pokedexConfig.load();
            pokedexConfig.save(); // Save after load to ensure new fields are written
            
            LOGGER.info("Loading reward configuration...");
            rewardConfig = new RewardConfig();
            rewardConfig.load();
            
            LOGGER.info("Loading player data configuration...");
            playerDataConfig = new PlayerDataConfig();
            
            // Initialize MessageHandler with configured prefix
            LOGGER.info("Initializing MessageHandler...");
            MessageHandler.initialize(pokedexConfig.getMessagePrefix());
            
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
            pokedexConfig.save(); // Save after load to ensure new fields are written
            rewardConfig.load();
            playerDataConfig = new PlayerDataConfig();
            permissions = new Permissions();
            
            // Reinitialize MessageHandler with updated prefix
            MessageHandler.initialize(pokedexConfig.getMessagePrefix());
            
            LOGGER.info("Cobblemon Pokedex Progression reloaded successfully");
        } catch (Exception e) {
            LOGGER.error("Failed to reload configurations", e);
            throw new RuntimeException("Failed to reload Cobblemon Pokedex", e);
        }
    }
}

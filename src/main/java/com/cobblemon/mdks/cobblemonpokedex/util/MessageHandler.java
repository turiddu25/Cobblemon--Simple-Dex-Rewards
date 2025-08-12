package com.cobblemon.mdks.cobblemonpokedex.util;

import com.cobblemon.mdks.cobblemonpokedex.CobblemonPokedex;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/**
 * Utility class for handling configurable message prefixes and formatting
 * throughout the SimpleDexRewards mod.
 */
public class MessageHandler {
    private static String prefix = "§b[§dSimpleDexRewards§b]§r ";
    
    /**
     * Initialize the MessageHandler with the configured prefix from PokedexConfig
     * @param configuredPrefix The prefix from configuration, or null to use default
     */
    public static void initialize(String configuredPrefix) {
        if (configuredPrefix != null && !configuredPrefix.trim().isEmpty()) {
            prefix = validatePrefix(configuredPrefix);
        } else {
            prefix = "§b[§dSimpleDexRewards§b]§r ";
        }
        CobblemonPokedex.LOGGER.info("MessageHandler initialized with prefix: " + prefix);
    }
    
    /**
     * Validates and sanitizes the message prefix
     * @param inputPrefix The prefix to validate
     * @return A valid prefix string
     */
    private static String validatePrefix(String inputPrefix) {
        if (inputPrefix == null || inputPrefix.trim().isEmpty()) {
            return "§b[§dSimpleDexRewards§b]§r ";
        }
        
        // Ensure prefix ends with a space for proper formatting
        if (!inputPrefix.endsWith(" ")) {
            inputPrefix += " ";
        }
        
        return inputPrefix;
    }
    
    /**
     * Format a message with the configured prefix
     * @param message The message to format
     * @return Component with the prefixed message
     */
    public static Component formatMessage(String message) {
        return Component.literal(prefix + message);
    }
    
    /**
     * Format a message with the configured prefix and color
     * @param message The message to format
     * @param color The ChatFormatting color to apply
     * @return Component with the prefixed and colored message
     */
    public static Component formatMessage(String message, ChatFormatting color) {
        return Component.literal(prefix + message).withStyle(color);
    }
    
    /**
     * Send a formatted message to a specific player
     * @param player The player to send the message to
     * @param message The message content
     */
    public static void sendToPlayer(ServerPlayer player, String message) {
        player.sendSystemMessage(formatMessage(message));
    }
    
    /**
     * Send a formatted message to a specific player with color
     * @param player The player to send the message to
     * @param message The message content
     * @param color The ChatFormatting color to apply
     */
    public static void sendToPlayer(ServerPlayer player, String message, ChatFormatting color) {
        player.sendSystemMessage(formatMessage(message, color));
    }
    
    /**
     * Broadcast a formatted message to all online players on the server
     * @param server The MinecraftServer instance
     * @param message The message to broadcast
     */
    public static void broadcastToServer(MinecraftServer server, String message) {
        if (server == null) {
            CobblemonPokedex.LOGGER.warn("Cannot broadcast message - server is null: " + message);
            return;
        }
        
        Component formattedMessage = formatMessage(message);
        server.getPlayerList().broadcastSystemMessage(formattedMessage, false);
    }
    
    /**
     * Broadcast a formatted message to all online players on the server with color
     * @param server The MinecraftServer instance
     * @param message The message to broadcast
     * @param color The ChatFormatting color to apply
     */
    public static void broadcastToServer(MinecraftServer server, String message, ChatFormatting color) {
        if (server == null) {
            CobblemonPokedex.LOGGER.warn("Cannot broadcast message - server is null: " + message);
            return;
        }
        
        Component formattedMessage = formatMessage(message, color);
        server.getPlayerList().broadcastSystemMessage(formattedMessage, false);
    }
    
    /**
     * Get the current configured prefix
     * @return The current message prefix
     */
    public static String getPrefix() {
        return prefix;
    }
}
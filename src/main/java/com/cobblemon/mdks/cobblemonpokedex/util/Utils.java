package com.cobblemon.mdks.cobblemonpokedex.util;

import com.cobblemon.mdks.cobblemonpokedex.CobblemonPokedex;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;

import java.io.File;
import java.io.FileWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Utils {
    public static boolean hasPermission(ServerPlayer player, String permission) {
        return player.hasPermissions(4) || player.getServer().getPlayerList().isOp(player.getGameProfile());
    }

    public static void sendMessage(ServerPlayer player, String message, ChatFormatting color) {
        player.sendSystemMessage(Component.literal(message).withStyle(color));
    }

    public static String readFileSync(String path, String fileName) {
        try {
            // Ensure directory exists
            File dir = checkForDirectory(path);
            Path filePath = Paths.get(dir.getAbsolutePath(), fileName);
            File file = filePath.toFile();

            if (!file.exists()) {
                return "";
            }

            return Files.readString(filePath, StandardCharsets.UTF_8);
        } catch (Exception e) {
            CobblemonPokedex.LOGGER.error("Failed to read file: " + fileName, e);
            return "";
        }
    }

    public static boolean writeFileSync(String path, String fileName, String content) {
        try {
            // Ensure directory exists
            File dir = checkForDirectory(path);
            Path filePath = Paths.get(dir.getAbsolutePath(), fileName);

            // Write file
            try (FileWriter writer = new FileWriter(filePath.toFile())) {
                writer.write(content);
            }
            File file = filePath.toFile();
            CobblemonPokedex.LOGGER.info("Successfully wrote file: " + filePath.toAbsolutePath());
            if (!file.exists()) {
                CobblemonPokedex.LOGGER.warn("File was written but does not exist: " + filePath.toAbsolutePath());
                return false;
            }
            return true;
        } catch (Exception e) {
            CobblemonPokedex.LOGGER.error("Failed to write file: " + fileName, e);
            return false;
        }
    }

    public static File checkForDirectory(String path) {
            File dir = new File(path);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return dir;
    }

    public static Gson newGson() {
        return new GsonBuilder().setPrettyPrinting().create();
    }
}

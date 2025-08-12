package com.cobblemon.mdks.cobblemonpokedex.util;

import com.cobblemon.mdks.cobblemonpokedex.CobblemonPokedex;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Utility class for migrating player data from v1.0 to v2.0 format.
 * Handles version detection, backup creation, and data transformation.
 */
public class PlayerDataMigration {
    private static final String V1_VERSION = "1.0";
    private static final String V2_VERSION = "2.0";
    private static final String BACKUP_SUFFIX = "_backup_";
    
    /**
     * Detects the version of a player data file
     * @param json The JsonObject containing player data
     * @return The version string, defaults to "1.0" if not present
     */
    public static String detectVersion(JsonObject json) {
        if (json.has("version")) {
            return json.get("version").getAsString();
        }
        return V1_VERSION; // Default to v1.0 for legacy files
    }
    
    /**
     * Checks if migration is needed for the given data
     * @param json The JsonObject containing player data
     * @return true if migration is needed, false otherwise
     */
    public static boolean needsMigration(JsonObject json) {
        String version = detectVersion(json);
        return V1_VERSION.equals(version);
    }
    
    /**
     * Creates a backup of the original player data file before migration
     * @param originalFile The original player data file
     * @return true if backup was created successfully, false otherwise
     */
    public static boolean createBackup(File originalFile) {
        try {
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String backupName = originalFile.getName().replace(".json", BACKUP_SUFFIX + timestamp + ".json");
            File backupFile = new File(originalFile.getParent(), backupName);
            
            Files.copy(originalFile.toPath(), backupFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            CobblemonPokedex.LOGGER.info("Created backup: " + backupFile.getName());
            return true;
        } catch (IOException e) {
            CobblemonPokedex.LOGGER.error("Failed to create backup for: " + originalFile.getName(), e);
            return false;
        }
    }
    
    /**
     * Migrates player data from v1.0 to v2.0 format
     * @param v1Data The v1.0 JsonObject data
     * @return The migrated v2.0 JsonObject data
     */
    public static JsonObject migrateToV2(JsonObject v1Data) {
        JsonObject v2Data = new JsonObject();
        
        // Copy existing fields
        v2Data.addProperty("version", V2_VERSION);
        v2Data.addProperty("totalCaught", v1Data.has("totalCaught") ? v1Data.get("totalCaught").getAsInt() : 0);
        v2Data.addProperty("highestTierReached", v1Data.has("highestTierReached") ? v1Data.get("highestTierReached").getAsInt() : 0);
        
        // Copy claimed rewards
        if (v1Data.has("claimedRewards")) {
            v2Data.add("claimedRewards", v1Data.get("claimedRewards"));
        } else {
            v2Data.add("claimedRewards", new JsonObject());
        }
        
        // Add new v2.0 fields with default values
        v2Data.addProperty("totalShinyCaught", 0);
        v2Data.addProperty("highestShinyTierReached", 0);
        v2Data.add("claimedShinyRewards", new JsonObject());
        v2Data.add("livingDexSpecies", new JsonObject()); // Will be converted to Set<String> in PlayerData
        v2Data.add("claimedLivingDexRewards", new JsonObject());
        v2Data.addProperty("lastSaveTime", System.currentTimeMillis());
        
        return v2Data;
    }
    
    /**
     * Performs a complete migration of a player data file
     * @param playerFile The player data file to migrate
     * @param configPath The configuration path for file operations
     * @return true if migration was successful, false otherwise
     */
    public static boolean migratePlayerFile(File playerFile, String configPath) {
        try {
            // Read existing data
            String content = Utils.readFileSync(configPath, playerFile.getName());
            if (content == null || content.isEmpty()) {
                CobblemonPokedex.LOGGER.warn("Empty or null content for file: " + playerFile.getName());
                return false;
            }
            
            JsonObject originalData = JsonParser.parseString(content).getAsJsonObject();
            
            // Check if migration is needed
            if (!needsMigration(originalData)) {
                CobblemonPokedex.LOGGER.debug("File " + playerFile.getName() + " does not need migration");
                return true;
            }
            
            // Create backup
            if (!createBackup(playerFile)) {
                CobblemonPokedex.LOGGER.error("Failed to create backup for " + playerFile.getName() + ", aborting migration");
                return false;
            }
            
            // Perform migration
            JsonObject migratedData = migrateToV2(originalData);
            
            // Write migrated data
            String migratedJson = Utils.newGson().toJson(migratedData);
            Utils.writeFileSync(configPath, playerFile.getName(), migratedJson);
            
            CobblemonPokedex.LOGGER.info("Successfully migrated " + playerFile.getName() + " from v1.0 to v2.0");
            return true;
            
        } catch (Exception e) {
            CobblemonPokedex.LOGGER.error("Failed to migrate player file: " + playerFile.getName(), e);
            return false;
        }
    }
    
    /**
     * Validates that migrated data contains all required v2.0 fields
     * @param migratedData The migrated JsonObject to validate
     * @return true if all required fields are present, false otherwise
     */
    public static boolean validateMigratedData(JsonObject migratedData) {
        String[] requiredFields = {
            "version", "totalCaught", "highestTierReached", "claimedRewards",
            "totalShinyCaught", "highestShinyTierReached", "claimedShinyRewards",
            "livingDexSpecies", "claimedLivingDexRewards", "lastSaveTime"
        };
        
        for (String field : requiredFields) {
            if (!migratedData.has(field)) {
                CobblemonPokedex.LOGGER.error("Migrated data missing required field: " + field);
                return false;
            }
        }
        
        // Validate version is correct
        if (!V2_VERSION.equals(migratedData.get("version").getAsString())) {
            CobblemonPokedex.LOGGER.error("Migrated data has incorrect version: " + migratedData.get("version").getAsString());
            return false;
        }
        
        return true;
    }
    
    /**
     * Attempts to recover from a backup file if migration fails
     * @param originalFile The original file that failed migration
     * @return true if recovery was successful, false otherwise
     */
    public static boolean recoverFromBackup(File originalFile) {
        try {
            File parentDir = originalFile.getParentFile();
            String baseName = originalFile.getName().replace(".json", "");
            
            // Find the most recent backup
            File[] backups = parentDir.listFiles((dir, name) -> 
                name.startsWith(baseName + BACKUP_SUFFIX) && name.endsWith(".json"));
            
            if (backups == null || backups.length == 0) {
                CobblemonPokedex.LOGGER.error("No backup files found for recovery: " + originalFile.getName());
                return false;
            }
            
            // Get the most recent backup (assuming timestamp ordering)
            File mostRecentBackup = backups[0];
            for (File backup : backups) {
                if (backup.lastModified() > mostRecentBackup.lastModified()) {
                    mostRecentBackup = backup;
                }
            }
            
            // Restore from backup
            Files.copy(mostRecentBackup.toPath(), originalFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            CobblemonPokedex.LOGGER.info("Successfully recovered " + originalFile.getName() + " from backup: " + mostRecentBackup.getName());
            return true;
            
        } catch (IOException e) {
            CobblemonPokedex.LOGGER.error("Failed to recover from backup for: " + originalFile.getName(), e);
            return false;
        }
    }
}
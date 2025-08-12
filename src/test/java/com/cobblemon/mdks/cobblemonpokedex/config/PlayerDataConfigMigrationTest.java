package com.cobblemon.mdks.cobblemonpokedex.config;

import com.cobblemon.mdks.cobblemonpokedex.util.Utils;
import com.google.gson.JsonObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Path;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class PlayerDataConfigMigrationTest {

    @TempDir
    Path tempDir;

    private String originalConfigPath;

    @BeforeEach
    void setUp() {
        // Store original config path and set temp directory
        originalConfigPath = System.getProperty("user.dir");
        System.setProperty("user.dir", tempDir.toString());
    }

    private void createV1PlayerFile(UUID playerId, int totalCaught, int highestTier) {
        JsonObject v1Data = new JsonObject();
        v1Data.addProperty("version", "1.0");
        v1Data.addProperty("totalCaught", totalCaught);
        v1Data.addProperty("highestTierReached", highestTier);

        JsonObject claimedRewards = new JsonObject();
        claimedRewards.addProperty("10", true);
        claimedRewards.addProperty("20", highestTier >= 20);
        v1Data.add("claimedRewards", claimedRewards);

        String configPath = "config/simpledexrewards/players";
        String filename = playerId.toString() + ".json";
        Utils.writeFileSync(configPath, filename, Utils.newGson().toJson(v1Data));
    }

    private void createV2PlayerFile(UUID playerId, int totalCaught, int totalShiny) {
        JsonObject v2Data = new JsonObject();
        v2Data.addProperty("version", "2.0");
        v2Data.addProperty("totalCaught", totalCaught);
        v2Data.addProperty("totalShinyCaught", totalShiny);
        v2Data.addProperty("highestTierReached", 30);
        v2Data.addProperty("highestShinyTierReached", 10);
        v2Data.addProperty("lastSaveTime", System.currentTimeMillis());

        JsonObject claimedRewards = new JsonObject();
        claimedRewards.addProperty("10", true);
        v2Data.add("claimedRewards", claimedRewards);

        v2Data.add("claimedShinyRewards", new JsonObject());
        v2Data.add("claimedLivingDexRewards", new JsonObject());
        v2Data.add("livingDexSpecies", new JsonObject());

        String configPath = "config/simpledexrewards/players";
        String filename = playerId.toString() + ".json";
        Utils.writeFileSync(configPath, filename, Utils.newGson().toJson(v2Data));
    }

    @Test
    void testLoadWithV1Files_AutoMigration() {
        UUID player1 = UUID.randomUUID();
        UUID player2 = UUID.randomUUID();

        // Create V1 player files
        createV1PlayerFile(player1, 50, 30);
        createV1PlayerFile(player2, 75, 50);

        // Load configuration (should trigger migration)
        PlayerDataConfig config = new PlayerDataConfig();

        // Verify players were loaded
        PlayerDataConfig.PlayerData data1 = config.getPlayerData(player1);
        PlayerDataConfig.PlayerData data2 = config.getPlayerData(player2);

        assertNotNull(data1);
        assertNotNull(data2);

        // Verify V1 data was preserved
        assertEquals(50, data1.getTotalCaught());
        assertEquals(30, data1.getHighestTierReached());
        assertEquals(75, data2.getTotalCaught());
        assertEquals(50, data2.getHighestTierReached());

        // Verify V2 fields were added with defaults
        assertEquals("2.0", data1.getVersion());
        assertEquals(0, data1.getTotalShinyCaught());
        assertEquals(0, data1.getHighestShinyTierReached());
        assertTrue(data1.getLastSaveTime() > 0);
        assertTrue(data1.getLivingDexSpecies().isEmpty());

        // Verify claimed rewards were preserved
        assertTrue(data1.hasClaimedReward(10));
        assertTrue(data1.hasClaimedReward(20));
        assertFalse(data1.hasClaimedShinyReward(10)); // Should be false for new field
    }

    @Test
    void testLoadWithV2Files_NoMigration() {
        UUID player1 = UUID.randomUUID();

        // Create V2 player file
        createV2PlayerFile(player1, 100, 25);

        // Load configuration (should not trigger migration)
        PlayerDataConfig config = new PlayerDataConfig();

        // Verify player was loaded correctly
        PlayerDataConfig.PlayerData data1 = config.getPlayerData(player1);
        assertNotNull(data1);

        assertEquals("2.0", data1.getVersion());
        assertEquals(100, data1.getTotalCaught());
        assertEquals(25, data1.getTotalShinyCaught());
        assertEquals(30, data1.getHighestTierReached());
        assertEquals(10, data1.getHighestShinyTierReached());
    }

    @Test
    void testLoadWithMixedFiles() {
        UUID v1Player = UUID.randomUUID();
        UUID v2Player = UUID.randomUUID();

        // Create mixed version files
        createV1PlayerFile(v1Player, 40, 20);
        createV2PlayerFile(v2Player, 80, 15);

        // Load configuration
        PlayerDataConfig config = new PlayerDataConfig();

        // Verify both players were loaded correctly
        PlayerDataConfig.PlayerData v1Data = config.getPlayerData(v1Player);
        PlayerDataConfig.PlayerData v2Data = config.getPlayerData(v2Player);

        // V1 player should be migrated
        assertEquals("2.0", v1Data.getVersion());
        assertEquals(40, v1Data.getTotalCaught());
        assertEquals(0, v1Data.getTotalShinyCaught()); // Default for migrated

        // V2 player should remain unchanged
        assertEquals("2.0", v2Data.getVersion());
        assertEquals(80, v2Data.getTotalCaught());
        assertEquals(15, v2Data.getTotalShinyCaught());
    }

    @Test
    void testBackupCreationDuringMigration() {
        UUID playerId = UUID.randomUUID();
        createV1PlayerFile(playerId, 60, 40);

        // Load configuration (triggers migration)
        new PlayerDataConfig();

        // Verify backup was created
        File playerDir = new File("config/simpledexrewards/players");
        File[] backups = playerDir.listFiles((dir, name) -> 
            name.startsWith(playerId.toString() + "_backup_") && name.endsWith(".json"));

        assertNotNull(backups);
        assertTrue(backups.length > 0, "Backup file should have been created");
    }

    @Test
    void testNewPlayerData_V2Format() {
        PlayerDataConfig config = new PlayerDataConfig();
        UUID newPlayer = UUID.randomUUID();

        // Get new player data (should create V2 format)
        PlayerDataConfig.PlayerData newData = config.getPlayerData(newPlayer);

        assertEquals("2.0", newData.getVersion());
        assertEquals(0, newData.getTotalCaught());
        assertEquals(0, newData.getTotalShinyCaught());
        assertEquals(0, newData.getHighestTierReached());
        assertEquals(0, newData.getHighestShinyTierReached());
        assertTrue(newData.getLastSaveTime() > 0);
        assertTrue(newData.getLivingDexSpecies().isEmpty());
    }

    @Test
    void testSaveImmediately() {
        PlayerDataConfig config = new PlayerDataConfig();
        UUID playerId = UUID.randomUUID();

        PlayerDataConfig.PlayerData data = config.getPlayerData(playerId);
        data.updateTotalCaught(25);
        data.updateTotalShinyCaught(5);

        long beforeSave = System.currentTimeMillis();
        config.saveImmediately(playerId);
        long afterSave = System.currentTimeMillis();

        // Verify last save time was updated
        assertTrue(data.getLastSaveTime() >= beforeSave);
        assertTrue(data.getLastSaveTime() <= afterSave);

        // Verify file was written
        File playerFile = new File("config/simpledexrewards/players/" + playerId.toString() + ".json");
        assertTrue(playerFile.exists());
    }

    @Test
    void testEnhancedPlayerDataMethods() {
        PlayerDataConfig.PlayerData data = new PlayerDataConfig.PlayerData();

        // Test shiny reward methods
        assertFalse(data.hasClaimedShinyReward(10));
        data.setClaimedShinyReward(10, true);
        assertTrue(data.hasClaimedShinyReward(10));
        assertEquals(10, data.getHighestShinyTierReached());

        // Test living dex methods
        assertTrue(data.getLivingDexSpecies().isEmpty());
        data.addLivingDexSpecies("pikachu");
        data.addLivingDexSpecies("charizard");
        assertEquals(2, data.getLivingDexSpecies().size());
        assertTrue(data.getLivingDexSpecies().contains("pikachu"));

        data.removeLivingDexSpecies("pikachu");
        assertEquals(1, data.getLivingDexSpecies().size());
        assertFalse(data.getLivingDexSpecies().contains("pikachu"));

        // Test living dex rewards
        assertFalse(data.hasClaimedLivingDexReward(15));
        data.setClaimedLivingDexReward(15, true);
        assertTrue(data.hasClaimedLivingDexReward(15));
    }

    @Test
    void testJsonSerialization_V2Format() {
        PlayerDataConfig.PlayerData data = new PlayerDataConfig.PlayerData();
        data.updateTotalCaught(50);
        data.updateTotalShinyCaught(10);
        data.setClaimedReward(20, true);
        data.setClaimedShinyReward(10, true);
        data.addLivingDexSpecies("bulbasaur");
        data.setClaimedLivingDexReward(5, true);

        JsonObject json = data.toJson();

        // Verify all V2 fields are present
        assertEquals("2.0", json.get("version").getAsString());
        assertEquals(50, json.get("totalCaught").getAsInt());
        assertEquals(10, json.get("totalShinyCaught").getAsInt());
        assertTrue(json.has("lastSaveTime"));

        // Verify reward structures
        assertTrue(json.getAsJsonObject("claimedRewards").get("20").getAsBoolean());
        assertTrue(json.getAsJsonObject("claimedShinyRewards").get("10").getAsBoolean());
        assertTrue(json.getAsJsonObject("claimedLivingDexRewards").get("5").getAsBoolean());
        assertTrue(json.getAsJsonObject("livingDexSpecies").get("bulbasaur").getAsBoolean());
    }
}
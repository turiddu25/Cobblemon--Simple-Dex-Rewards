package com.cobblemon.mdks.cobblemonpokedex.util;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class PlayerDataMigrationTest {

    @TempDir
    Path tempDir;

    private JsonObject createV1Data() {
        JsonObject v1Data = new JsonObject();
        v1Data.addProperty("version", "1.0");
        v1Data.addProperty("totalCaught", 50);
        v1Data.addProperty("highestTierReached", 30);
        
        JsonObject claimedRewards = new JsonObject();
        claimedRewards.addProperty("10", true);
        claimedRewards.addProperty("20", true);
        claimedRewards.addProperty("30", false);
        v1Data.add("claimedRewards", claimedRewards);
        
        return v1Data;
    }

    private JsonObject createV2Data() {
        JsonObject v2Data = new JsonObject();
        v2Data.addProperty("version", "2.0");
        v2Data.addProperty("totalCaught", 75);
        v2Data.addProperty("totalShinyCaught", 15);
        v2Data.addProperty("highestTierReached", 50);
        v2Data.addProperty("highestShinyTierReached", 10);
        v2Data.addProperty("lastSaveTime", System.currentTimeMillis());
        
        JsonObject claimedRewards = new JsonObject();
        claimedRewards.addProperty("10", true);
        claimedRewards.addProperty("20", true);
        v2Data.add("claimedRewards", claimedRewards);
        
        JsonObject claimedShinyRewards = new JsonObject();
        claimedShinyRewards.addProperty("10", true);
        v2Data.add("claimedShinyRewards", claimedShinyRewards);
        
        v2Data.add("livingDexSpecies", new JsonObject());
        v2Data.add("claimedLivingDexRewards", new JsonObject());
        
        return v2Data;
    }

    @Test
    void testDetectVersion_V1() {
        JsonObject v1Data = createV1Data();
        String version = PlayerDataMigration.detectVersion(v1Data);
        assertEquals("1.0", version);
    }

    @Test
    void testDetectVersion_V2() {
        JsonObject v2Data = createV2Data();
        String version = PlayerDataMigration.detectVersion(v2Data);
        assertEquals("2.0", version);
    }

    @Test
    void testDetectVersion_NoVersion() {
        JsonObject noVersionData = new JsonObject();
        noVersionData.addProperty("totalCaught", 25);
        String version = PlayerDataMigration.detectVersion(noVersionData);
        assertEquals("1.0", version); // Should default to 1.0
    }

    @Test
    void testNeedsMigration_V1() {
        JsonObject v1Data = createV1Data();
        assertTrue(PlayerDataMigration.needsMigration(v1Data));
    }

    @Test
    void testNeedsMigration_V2() {
        JsonObject v2Data = createV2Data();
        assertFalse(PlayerDataMigration.needsMigration(v2Data));
    }

    @Test
    void testNeedsMigration_NoVersion() {
        JsonObject noVersionData = new JsonObject();
        noVersionData.addProperty("totalCaught", 25);
        assertTrue(PlayerDataMigration.needsMigration(noVersionData));
    }

    @Test
    void testCreateBackup() throws IOException {
        // Create a test file
        File testFile = tempDir.resolve("test-player.json").toFile();
        Files.write(testFile.toPath(), "test content".getBytes());

        // Create backup
        boolean result = PlayerDataMigration.createBackup(testFile);
        assertTrue(result);

        // Verify backup was created
        File[] backups = testFile.getParentFile().listFiles((dir, name) -> 
            name.startsWith("test-player_backup_") && name.endsWith(".json"));
        assertNotNull(backups);
        assertEquals(1, backups.length);

        // Verify backup content
        String backupContent = Files.readString(backups[0].toPath());
        assertEquals("test content", backupContent);
    }

    @Test
    void testMigrateToV2_BasicFields() {
        JsonObject v1Data = createV1Data();
        JsonObject migratedData = PlayerDataMigration.migrateToV2(v1Data);

        // Verify version was updated
        assertEquals("2.0", migratedData.get("version").getAsString());

        // Verify existing fields were preserved
        assertEquals(50, migratedData.get("totalCaught").getAsInt());
        assertEquals(30, migratedData.get("highestTierReached").getAsInt());

        // Verify claimed rewards were preserved
        JsonObject claimedRewards = migratedData.getAsJsonObject("claimedRewards");
        assertTrue(claimedRewards.get("10").getAsBoolean());
        assertTrue(claimedRewards.get("20").getAsBoolean());
        assertFalse(claimedRewards.get("30").getAsBoolean());
    }

    @Test
    void testMigrateToV2_NewFields() {
        JsonObject v1Data = createV1Data();
        JsonObject migratedData = PlayerDataMigration.migrateToV2(v1Data);

        // Verify new fields were added with default values
        assertEquals(0, migratedData.get("totalShinyCaught").getAsInt());
        assertEquals(0, migratedData.get("highestShinyTierReached").getAsInt());
        assertTrue(migratedData.has("lastSaveTime"));
        assertTrue(migratedData.get("lastSaveTime").getAsLong() > 0);

        // Verify new reward maps were created
        assertTrue(migratedData.has("claimedShinyRewards"));
        assertTrue(migratedData.has("claimedLivingDexRewards"));
        assertTrue(migratedData.has("livingDexSpecies"));

        JsonObject claimedShinyRewards = migratedData.getAsJsonObject("claimedShinyRewards");
        assertEquals(0, claimedShinyRewards.size());
    }

    @Test
    void testMigrateToV2_EmptyV1Data() {
        JsonObject emptyV1Data = new JsonObject();
        emptyV1Data.addProperty("version", "1.0");
        
        JsonObject migratedData = PlayerDataMigration.migrateToV2(emptyV1Data);

        // Verify version was updated
        assertEquals("2.0", migratedData.get("version").getAsString());

        // Verify default values were set
        assertEquals(0, migratedData.get("totalCaught").getAsInt());
        assertEquals(0, migratedData.get("highestTierReached").getAsInt());
        assertEquals(0, migratedData.get("totalShinyCaught").getAsInt());
        assertEquals(0, migratedData.get("highestShinyTierReached").getAsInt());

        // Verify empty reward maps were created
        JsonObject claimedRewards = migratedData.getAsJsonObject("claimedRewards");
        assertEquals(0, claimedRewards.size());
    }

    @Test
    void testValidateMigratedData_Valid() {
        JsonObject validV2Data = createV2Data();
        assertTrue(PlayerDataMigration.validateMigratedData(validV2Data));
    }

    @Test
    void testValidateMigratedData_MissingField() {
        JsonObject invalidData = createV2Data();
        invalidData.remove("totalShinyCaught");
        assertFalse(PlayerDataMigration.validateMigratedData(invalidData));
    }

    @Test
    void testValidateMigratedData_WrongVersion() {
        JsonObject invalidData = createV2Data();
        invalidData.addProperty("version", "1.0");
        assertFalse(PlayerDataMigration.validateMigratedData(invalidData));
    }

    @Test
    void testRecoverFromBackup() throws IOException {
        // Create original file
        File originalFile = tempDir.resolve("player.json").toFile();
        Files.write(originalFile.toPath(), "original content".getBytes());

        // Create backup
        PlayerDataMigration.createBackup(originalFile);

        // Corrupt original file
        Files.write(originalFile.toPath(), "corrupted content".getBytes());

        // Recover from backup
        boolean result = PlayerDataMigration.recoverFromBackup(originalFile);
        assertTrue(result);

        // Verify recovery
        String recoveredContent = Files.readString(originalFile.toPath());
        assertEquals("original content", recoveredContent);
    }

    @Test
    void testRecoverFromBackup_NoBackup() throws IOException {
        // Create original file without backup
        File originalFile = tempDir.resolve("player.json").toFile();
        Files.write(originalFile.toPath(), "original content".getBytes());

        // Try to recover (should fail)
        boolean result = PlayerDataMigration.recoverFromBackup(originalFile);
        assertFalse(result);
    }

    @Test
    void testMigrateToV2_PreservesComplexRewards() {
        JsonObject v1Data = new JsonObject();
        v1Data.addProperty("version", "1.0");
        v1Data.addProperty("totalCaught", 100);
        v1Data.addProperty("highestTierReached", 80);

        // Create complex claimed rewards structure
        JsonObject claimedRewards = new JsonObject();
        claimedRewards.addProperty("10", true);
        claimedRewards.addProperty("25", true);
        claimedRewards.addProperty("50", false);
        claimedRewards.addProperty("75", true);
        claimedRewards.addProperty("100", false);
        v1Data.add("claimedRewards", claimedRewards);

        JsonObject migratedData = PlayerDataMigration.migrateToV2(v1Data);

        // Verify all claimed rewards were preserved
        JsonObject migratedRewards = migratedData.getAsJsonObject("claimedRewards");
        assertEquals(5, migratedRewards.size());
        assertTrue(migratedRewards.get("10").getAsBoolean());
        assertTrue(migratedRewards.get("25").getAsBoolean());
        assertFalse(migratedRewards.get("50").getAsBoolean());
        assertTrue(migratedRewards.get("75").getAsBoolean());
        assertFalse(migratedRewards.get("100").getAsBoolean());
    }

    @Test
    void testMigrateToV2_HandlesNullValues() {
        JsonObject v1Data = new JsonObject();
        v1Data.addProperty("version", "1.0");
        // Don't add totalCaught or highestTierReached to test null handling

        JsonObject migratedData = PlayerDataMigration.migrateToV2(v1Data);

        // Verify defaults were used for missing fields
        assertEquals(0, migratedData.get("totalCaught").getAsInt());
        assertEquals(0, migratedData.get("highestTierReached").getAsInt());
        assertEquals("2.0", migratedData.get("version").getAsString());

        // Verify new fields were added
        assertTrue(migratedData.has("totalShinyCaught"));
        assertTrue(migratedData.has("claimedShinyRewards"));
        assertTrue(migratedData.has("livingDexSpecies"));
    }
}
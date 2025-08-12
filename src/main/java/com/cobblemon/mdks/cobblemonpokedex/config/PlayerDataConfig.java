package com.cobblemon.mdks.cobblemonpokedex.config;

import com.cobblemon.mdks.cobblemonpokedex.CobblemonPokedex;
import com.cobblemon.mdks.cobblemonpokedex.util.PlayerDataMigration;
import com.cobblemon.mdks.cobblemonpokedex.util.Utils;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class PlayerDataConfig {
    private Map<UUID, PlayerData> playerDataMap = new HashMap<>();
    private static final String CONFIG_PATH = "config/simpledexrewards/players";
    private static final String DATA_VERSION = "2.0";

    public PlayerDataConfig() {
        File playerDir = Utils.checkForDirectory("/" + CONFIG_PATH);
        CobblemonPokedex.LOGGER.info("Loading player data configuration...");
        load();
    }

    public void load() {
        File playerDir = new File(CONFIG_PATH);
        if (!playerDir.exists()) {
            playerDir.mkdirs();
            CobblemonPokedex.LOGGER.info("Created players directory at " + playerDir.getAbsolutePath());
        }

        File[] files = playerDir.listFiles((dir, name) -> 
            name.endsWith(".json") && !name.contains("_backup_"));
        if (files == null) return;

        int migratedCount = 0;
        int failedMigrations = 0;

        for (File file : files) {
            try {
                String content = Utils.readFileSync(CONFIG_PATH, file.getName());
                if (content == null || content.isEmpty()) continue;

                JsonObject json = JsonParser.parseString(content).getAsJsonObject();
                
                // Check if migration is needed
                if (PlayerDataMigration.needsMigration(json)) {
                    CobblemonPokedex.LOGGER.info("Migrating player data file: " + file.getName());
                    if (PlayerDataMigration.migratePlayerFile(file, CONFIG_PATH)) {
                        migratedCount++;
                        // Re-read the migrated data
                        content = Utils.readFileSync(CONFIG_PATH, file.getName());
                        json = JsonParser.parseString(content).getAsJsonObject();
                    } else {
                        failedMigrations++;
                        CobblemonPokedex.LOGGER.error("Failed to migrate player data: " + file.getName());
                        continue;
                    }
                }

                UUID playerId = UUID.fromString(file.getName().replace(".json", ""));
                PlayerData data = new PlayerData(json);
                playerDataMap.put(playerId, data);
            } catch (Exception e) {
                CobblemonPokedex.LOGGER.error("Failed to load player data: " + file.getName(), e);
            }
        }

        if (migratedCount > 0) {
            CobblemonPokedex.LOGGER.info("Successfully migrated " + migratedCount + " player data files to v2.0");
        }
        if (failedMigrations > 0) {
            CobblemonPokedex.LOGGER.warn("Failed to migrate " + failedMigrations + " player data files");
        }
    }

    public void save() {
        for (Map.Entry<UUID, PlayerData> entry : playerDataMap.entrySet()) {
            String filename = entry.getKey().toString() + ".json";
            Utils.writeFileSync(CONFIG_PATH, filename, Utils.newGson().toJson(entry.getValue().toJson()));
        }
    }

    public void savePlayer(UUID playerId) {
        PlayerData data = playerDataMap.get(playerId);
        if (data != null) {
            String filename = playerId.toString() + ".json";
            Utils.writeFileSync(CONFIG_PATH, filename, Utils.newGson().toJson(data.toJson()));
        }
    }

    public PlayerData getPlayerData(UUID playerId) {
        return playerDataMap.computeIfAbsent(playerId, k -> new PlayerData());
    }

    /**
     * Immediately saves a specific player's data to disk
     * @param playerId The UUID of the player whose data should be saved
     */
    public void saveImmediately(UUID playerId) {
        PlayerData data = playerDataMap.get(playerId);
        if (data != null) {
            data.updateLastSaveTime();
            savePlayer(playerId);
        }
    }

    public static class PlayerData {
        private final String version;
        private final Map<Integer, Boolean> claimedRewards;
        private final Map<Integer, Boolean> claimedShinyRewards;
        private final Map<Integer, Boolean> claimedLivingDexRewards;
        private final Set<String> livingDexSpecies;
        private int totalCaught;
        private int totalShinyCaught;
        private int highestTierReached;
        private int highestShinyTierReached;
        private long lastSaveTime;

        public PlayerData() {
            this.version = DATA_VERSION;
            this.claimedRewards = new HashMap<>();
            this.claimedShinyRewards = new HashMap<>();
            this.claimedLivingDexRewards = new HashMap<>();
            this.livingDexSpecies = new HashSet<>();
            this.totalCaught = 0;
            this.totalShinyCaught = 0;
            this.highestTierReached = 0;
            this.highestShinyTierReached = 0;
            this.lastSaveTime = System.currentTimeMillis();
        }

        public PlayerData(JsonObject json) {
            this.version = json.has("version") ? json.get("version").getAsString() : DATA_VERSION;
            this.claimedRewards = new HashMap<>();
            this.claimedShinyRewards = new HashMap<>();
            this.claimedLivingDexRewards = new HashMap<>();
            this.livingDexSpecies = new HashSet<>();
            
            this.totalCaught = json.has("totalCaught") ? json.get("totalCaught").getAsInt() : 0;
            this.totalShinyCaught = json.has("totalShinyCaught") ? json.get("totalShinyCaught").getAsInt() : 0;
            this.highestTierReached = json.has("highestTierReached") ? json.get("highestTierReached").getAsInt() : 0;
            this.highestShinyTierReached = json.has("highestShinyTierReached") ? json.get("highestShinyTierReached").getAsInt() : 0;
            this.lastSaveTime = json.has("lastSaveTime") ? json.get("lastSaveTime").getAsLong() : System.currentTimeMillis();

            // Load claimed rewards
            if (json.has("claimedRewards")) {
                JsonObject rewards = json.getAsJsonObject("claimedRewards");
                for (String key : rewards.keySet()) {
                    int tier = Integer.parseInt(key);
                    boolean claimed = rewards.get(key).getAsBoolean();
                    claimedRewards.put(tier, claimed);
                }
            }

            // Load claimed shiny rewards
            if (json.has("claimedShinyRewards")) {
                JsonObject shinyRewards = json.getAsJsonObject("claimedShinyRewards");
                for (String key : shinyRewards.keySet()) {
                    int tier = Integer.parseInt(key);
                    boolean claimed = shinyRewards.get(key).getAsBoolean();
                    claimedShinyRewards.put(tier, claimed);
                }
            }

            // Load claimed living dex rewards
            if (json.has("claimedLivingDexRewards")) {
                JsonObject livingDexRewards = json.getAsJsonObject("claimedLivingDexRewards");
                for (String key : livingDexRewards.keySet()) {
                    int tier = Integer.parseInt(key);
                    boolean claimed = livingDexRewards.get(key).getAsBoolean();
                    claimedLivingDexRewards.put(tier, claimed);
                }
            }

            // Load living dex species (stored as JsonObject keys for compatibility)
            if (json.has("livingDexSpecies")) {
                JsonObject livingDex = json.getAsJsonObject("livingDexSpecies");
                for (String species : livingDex.keySet()) {
                    livingDexSpecies.add(species);
                }
            }
        }

        public boolean hasClaimedReward(int tier) {
            return claimedRewards.getOrDefault(tier, false);
        }

        public void setClaimedReward(int tier, boolean claimed) {
            claimedRewards.put(tier, claimed);
            if (claimed && tier > highestTierReached) {
                highestTierReached = tier;
            }
            updateLastSaveTime();
        }

        public boolean hasClaimedShinyReward(int tier) {
            return claimedShinyRewards.getOrDefault(tier, false);
        }

        public void setClaimedShinyReward(int tier, boolean claimed) {
            claimedShinyRewards.put(tier, claimed);
            if (claimed && tier > highestShinyTierReached) {
                highestShinyTierReached = tier;
            }
            updateLastSaveTime();
        }

        public boolean hasClaimedLivingDexReward(int tier) {
            return claimedLivingDexRewards.getOrDefault(tier, false);
        }

        public void setClaimedLivingDexReward(int tier, boolean claimed) {
            claimedLivingDexRewards.put(tier, claimed);
            updateLastSaveTime();
        }

        public void updateTotalCaught(int count) {
            this.totalCaught = count;
            updateLastSaveTime();
        }

        public void updateTotalShinyCaught(int count) {
            this.totalShinyCaught = count;
            updateLastSaveTime();
        }

        public void updateLivingDexSpecies(Set<String> species) {
            this.livingDexSpecies.clear();
            this.livingDexSpecies.addAll(species);
            updateLastSaveTime();
        }

        public void addLivingDexSpecies(String species) {
            this.livingDexSpecies.add(species);
            updateLastSaveTime();
        }

        public void removeLivingDexSpecies(String species) {
            this.livingDexSpecies.remove(species);
            updateLastSaveTime();
        }

        private void updateLastSaveTime() {
            this.lastSaveTime = System.currentTimeMillis();
        }

        public int getTotalCaught() {
            return totalCaught;
        }

        public int getTotalShinyCaught() {
            return totalShinyCaught;
        }

        public int getHighestTierReached() {
            return highestTierReached;
        }

        public int getHighestShinyTierReached() {
            return highestShinyTierReached;
        }

        public Set<String> getLivingDexSpecies() {
            return new HashSet<>(livingDexSpecies);
        }

        public long getLastSaveTime() {
            return lastSaveTime;
        }

        public String getVersion() {
            return version;
        }

        public JsonObject toJson() {
            JsonObject json = new JsonObject();
            json.addProperty("version", version);
            json.addProperty("totalCaught", totalCaught);
            json.addProperty("totalShinyCaught", totalShinyCaught);
            json.addProperty("highestTierReached", highestTierReached);
            json.addProperty("highestShinyTierReached", highestShinyTierReached);
            json.addProperty("lastSaveTime", lastSaveTime);

            // Regular claimed rewards
            JsonObject rewards = new JsonObject();
            for (Map.Entry<Integer, Boolean> entry : claimedRewards.entrySet()) {
                rewards.addProperty(entry.getKey().toString(), entry.getValue());
            }
            json.add("claimedRewards", rewards);

            // Shiny claimed rewards
            JsonObject shinyRewards = new JsonObject();
            for (Map.Entry<Integer, Boolean> entry : claimedShinyRewards.entrySet()) {
                shinyRewards.addProperty(entry.getKey().toString(), entry.getValue());
            }
            json.add("claimedShinyRewards", shinyRewards);

            // Living dex claimed rewards
            JsonObject livingDexRewards = new JsonObject();
            for (Map.Entry<Integer, Boolean> entry : claimedLivingDexRewards.entrySet()) {
                livingDexRewards.addProperty(entry.getKey().toString(), entry.getValue());
            }
            json.add("claimedLivingDexRewards", livingDexRewards);

            // Living dex species (stored as JsonObject keys for compatibility)
            JsonObject livingDex = new JsonObject();
            for (String species : livingDexSpecies) {
                livingDex.addProperty(species, true);
            }
            json.add("livingDexSpecies", livingDex);

            return json;
        }
    }
}

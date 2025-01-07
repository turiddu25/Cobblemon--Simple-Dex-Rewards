package com.cobblemon.mdks.cobblemonpokedex.config;

import com.cobblemon.mdks.cobblemonpokedex.CobblemonPokedex;
import com.cobblemon.mdks.cobblemonpokedex.util.Utils;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PlayerDataConfig {
    private Map<UUID, PlayerData> playerDataMap = new HashMap<>();
    private static final String CONFIG_PATH = "config/simpledexrewards/players";
    private static final String DATA_VERSION = "1.0";

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

        File[] files = playerDir.listFiles((dir, name) -> name.endsWith(".json"));
        if (files == null) return;

        for (File file : files) {
            try {
                String content = Utils.readFileSync(CONFIG_PATH, file.getName());
                if (content == null || content.isEmpty()) continue;

                UUID playerId = UUID.fromString(file.getName().replace(".json", ""));
                JsonObject json = JsonParser.parseString(content).getAsJsonObject();
                PlayerData data = new PlayerData(json);
                playerDataMap.put(playerId, data);
            } catch (Exception e) {
                CobblemonPokedex.LOGGER.error("Failed to load player data: " + file.getName(), e);
            }
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

    public static class PlayerData {
        private final String version;
        private final Map<Integer, Boolean> claimedRewards;
        private int totalCaught;
        private int highestTierReached;

        public PlayerData() {
            this.version = DATA_VERSION;
            this.claimedRewards = new HashMap<>();
            this.totalCaught = 0;
            this.highestTierReached = 0;
        }

        public PlayerData(JsonObject json) {
            this.version = json.has("version") ? json.get("version").getAsString() : DATA_VERSION;
            this.claimedRewards = new HashMap<>();
            this.totalCaught = json.has("totalCaught") ? json.get("totalCaught").getAsInt() : 0;
            this.highestTierReached = json.has("highestTierReached") ? json.get("highestTierReached").getAsInt() : 0;

            if (json.has("claimedRewards")) {
                JsonObject rewards = json.getAsJsonObject("claimedRewards");
                for (String key : rewards.keySet()) {
                    int tier = Integer.parseInt(key);
                    boolean claimed = rewards.get(key).getAsBoolean();
                    claimedRewards.put(tier, claimed);
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
        }

        public void updateTotalCaught(int count) {
            this.totalCaught = count;
        }

        public int getTotalCaught() {
            return totalCaught;
        }

        public int getHighestTierReached() {
            return highestTierReached;
        }

        public JsonObject toJson() {
            JsonObject json = new JsonObject();
            json.addProperty("version", version);
            json.addProperty("totalCaught", totalCaught);
            json.addProperty("highestTierReached", highestTierReached);

            JsonObject rewards = new JsonObject();
            for (Map.Entry<Integer, Boolean> entry : claimedRewards.entrySet()) {
                rewards.addProperty(entry.getKey().toString(), entry.getValue());
            }
            json.add("claimedRewards", rewards);

            return json;
        }
    }
}

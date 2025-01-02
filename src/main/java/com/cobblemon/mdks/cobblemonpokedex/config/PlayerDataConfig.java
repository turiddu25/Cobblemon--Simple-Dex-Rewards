package com.cobblemon.mdks.cobblemonpokedex.config;

import com.cobblemon.mdks.cobblemonpokedex.CobblemonPokedex;
import com.cobblemon.mdks.cobblemonpokedex.util.Utils;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PlayerDataConfig {
    private Map<UUID, PlayerData> playerDataMap = new HashMap<>();
    private static final String CONFIG_FILE = "player_data.json";

    public PlayerDataConfig() {
        load();
    }

    public void load() {
        String content = Utils.readFileSync("config/cobblemonpokedex", CONFIG_FILE);
        if (content == null || content.isEmpty()) {
            return;
        }

        try {
            JsonObject json = JsonParser.parseString(content).getAsJsonObject();
            for (String key : json.keySet()) {
                UUID playerId = UUID.fromString(key);
                PlayerData data = new PlayerData(json.getAsJsonObject(key));
                playerDataMap.put(playerId, data);
            }
        } catch (Exception e) {
            CobblemonPokedex.LOGGER.error("Failed to load player data config", e);
        }
    }

    public void save() {
        JsonObject json = new JsonObject();
        for (Map.Entry<UUID, PlayerData> entry : playerDataMap.entrySet()) {
            json.add(entry.getKey().toString(), entry.getValue().toJson());
        }

        Utils.writeFileSync("config/cobblemonpokedex", CONFIG_FILE,
                Utils.newGson().toJson(json));
    }

    public PlayerData getPlayerData(UUID playerId) {
        return playerDataMap.computeIfAbsent(playerId, k -> new PlayerData());
    }

    public static class PlayerData {
        private Map<Integer, Boolean> claimedRewards = new HashMap<>();

        public PlayerData() {}

        public PlayerData(JsonObject json) {
            for (String key : json.keySet()) {
                int tier = Integer.parseInt(key);
                boolean claimed = json.get(key).getAsBoolean();
                claimedRewards.put(tier, claimed);
            }
        }

        public boolean hasClaimedReward(int tier) {
            return claimedRewards.getOrDefault(tier, false);
        }

        public void setClaimedReward(int tier, boolean claimed) {
            claimedRewards.put(tier, claimed);
        }

        public JsonObject toJson() {
            JsonObject json = new JsonObject();
            for (Map.Entry<Integer, Boolean> entry : claimedRewards.entrySet()) {
                json.addProperty(entry.getKey().toString(), entry.getValue());
            }
            return json;
        }
    }
}

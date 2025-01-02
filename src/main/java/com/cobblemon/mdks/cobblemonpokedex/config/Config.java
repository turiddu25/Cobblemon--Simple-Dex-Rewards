package com.cobblemon.mdks.cobblemonpokedex.config;

import com.cobblemon.mdks.cobblemonpokedex.CobblemonPokedex;
import com.cobblemon.mdks.cobblemonpokedex.util.*;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class Config {
    private int[] completionTiers;
    private boolean enablePermissionNodes;
    private JsonObject rewards;

    public Config() {
        setDefaults();
    }

    private void setDefaults() {
        // Default completion tiers at 25%, 50%, 75%, 100%
        this.completionTiers = new int[]{25, 50, 75, 100};
        this.enablePermissionNodes = true;
        this.rewards = new JsonObject();
        
        // Initialize default empty rewards
        for (int tier : completionTiers) {
            rewards.add(String.valueOf(tier), new JsonArray());
        }
    }

    public void load() {
        String content = Utils.readFileSync(Constants.CONFIG_PATH, Constants.CONFIG_FILE);
        if (content == null || content.isEmpty()) {
            setDefaults();
            save();
            return;
        }

        try {
            JsonObject json = JsonParser.parseString(content).getAsJsonObject();
            loadFromJson(json);
        } catch (Exception e) {
            CobblemonPokedex.LOGGER.error("Failed to load config", e);
            setDefaults();
            save();
        }
    }

    private void loadFromJson(JsonObject json) {
        if (json.has("completionTiers")) {
            JsonArray tiersArray = json.getAsJsonArray("completionTiers");
            this.completionTiers = new int[tiersArray.size()];
            for (int i = 0; i < tiersArray.size(); i++) {
                this.completionTiers[i] = tiersArray.get(i).getAsInt();
            }
        }
        
        this.enablePermissionNodes = getOrDefault(json, "enablePermissionNodes", true);
        this.rewards = json.has("rewards") ? json.getAsJsonObject("rewards") : new JsonObject();
    }

    private <T> T getOrDefault(JsonObject json, String key, T defaultValue) {
        if (!json.has(key)) return defaultValue;
        
        JsonElement element = json.get(key);
        if (defaultValue instanceof Integer) {
            return (T) Integer.valueOf(element.getAsInt());
        } else if (defaultValue instanceof Boolean) {
            return (T) Boolean.valueOf(element.getAsBoolean());
        }
        return defaultValue;
    }

    public void save() {
        JsonObject json = new JsonObject();
        
        JsonArray tiersArray = new JsonArray();
        for (int tier : completionTiers) {
            tiersArray.add(tier);
        }
        json.add("completionTiers", tiersArray);
        
        json.addProperty("enablePermissionNodes", enablePermissionNodes);
        json.add("rewards", rewards);

        Utils.writeFileSync(Constants.CONFIG_PATH, Constants.CONFIG_FILE,
                Utils.newGson().toJson(json));
    }

    // Getters
    public int[] getCompletionTiers() { return completionTiers; }
    public boolean isEnablePermissionNodes() { return enablePermissionNodes; }
    public JsonObject getRewards() { return rewards; }
    
    /**
     * Gets the rewards for a specific completion tier
     * @param tier The completion tier percentage (e.g. 25, 50, 75, 100)
     * @return JsonArray of rewards for the tier
     */
    public JsonElement getRewardForTier(int tier) {
        String tierKey = String.valueOf(tier);
        if (rewards.has(tierKey)) {
            JsonArray tierRewards = rewards.getAsJsonArray(tierKey);
            if (tierRewards.size() > 0) {
                return tierRewards.get(0);
            }
        }
        return null;
    }

    public void setRewardForTier(int tier, JsonElement reward) {
        String tierKey = String.valueOf(tier);
        JsonArray tierRewards = new JsonArray();
        tierRewards.add(reward);
        rewards.add(tierKey, tierRewards);
        save();
    }
}

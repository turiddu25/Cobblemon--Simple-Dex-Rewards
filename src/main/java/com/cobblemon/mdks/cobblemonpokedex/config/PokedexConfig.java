package com.cobblemon.mdks.cobblemonpokedex.config;

import com.cobblemon.mdks.cobblemonpokedex.CobblemonPokedex;
import com.cobblemon.mdks.cobblemonpokedex.util.Utils;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class PokedexConfig {
    private static final String CONFIG_PATH = "config/simpledexrewards";
    private static final String CONFIG_FILE = "config.json";
    
    // Core settings
    private int maxTiers;
    private boolean enablePermissionNodes;
    private boolean savePlayerData;
    private String dataVersion;
    
    // Reward settings
    private boolean enableItemRewards;
    private boolean enablePokemonRewards;
    private boolean enableCommandRewards;
    
    public PokedexConfig() {
        setDefaults();
    }
    
    private void setDefaults() {
        this.maxTiers = 10;
        this.enablePermissionNodes = true;
        this.savePlayerData = true;
        this.dataVersion = "1.0";
        this.enableItemRewards = true;
        this.enablePokemonRewards = true;
        this.enableCommandRewards = true;
    }
    
    public void load() {
        // Ensure config directory exists
        Utils.checkForDirectory("/" + CONFIG_PATH);
        
        String content = Utils.readFileSync(CONFIG_PATH, CONFIG_FILE);
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
        this.maxTiers = getOrDefault(json, "maxTiers", 10);
        this.enablePermissionNodes = getOrDefault(json, "enablePermissionNodes", true);
        this.savePlayerData = getOrDefault(json, "savePlayerData", true);
        this.dataVersion = getOrDefault(json, "dataVersion", "1.0");
        this.enableItemRewards = getOrDefault(json, "enableItemRewards", true);
        this.enablePokemonRewards = getOrDefault(json, "enablePokemonRewards", true);
        this.enableCommandRewards = getOrDefault(json, "enableCommandRewards", true);
    }
    
    private <T> T getOrDefault(JsonObject json, String key, T defaultValue) {
        if (!json.has(key)) return defaultValue;
        
        if (defaultValue instanceof Boolean) {
            return (T) Boolean.valueOf(json.get(key).getAsBoolean());
        } else if (defaultValue instanceof Integer) {
            return (T) Integer.valueOf(json.get(key).getAsInt());
        } else if (defaultValue instanceof String) {
            return (T) json.get(key).getAsString();
        }
        return defaultValue;
    }
    
    public void save() {
        JsonObject json = new JsonObject();
        json.addProperty("maxTiers", maxTiers);
        json.addProperty("enablePermissionNodes", enablePermissionNodes);
        json.addProperty("savePlayerData", savePlayerData);
        json.addProperty("dataVersion", dataVersion);
        json.addProperty("enableItemRewards", enableItemRewards);
        json.addProperty("enablePokemonRewards", enablePokemonRewards);
        json.addProperty("enableCommandRewards", enableCommandRewards);
        
        Utils.writeFileSync(CONFIG_PATH, CONFIG_FILE, Utils.newGson().toJson(json));
    }
    
    // Getters
    public int getMaxTiers() { return maxTiers; }
    public boolean isEnablePermissionNodes() { return enablePermissionNodes; }
    public boolean isSavePlayerData() { return savePlayerData; }
    public String getDataVersion() { return dataVersion; }
    public boolean isEnableItemRewards() { return enableItemRewards; }
    public boolean isEnablePokemonRewards() { return enablePokemonRewards; }
    public boolean isEnableCommandRewards() { return enableCommandRewards; }
}

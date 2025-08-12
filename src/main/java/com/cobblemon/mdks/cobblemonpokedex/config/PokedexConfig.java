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
    private int totalPokemon;
    private boolean enablePermissionNodes;
    private boolean savePlayerData;
    private String dataVersion;
    
    // Enhanced configuration fields
    private String messagePrefix;
    private boolean enableShinyTracking;
    private boolean enableLivingDexTracking;
    private int totalShinyPokemon;
    
    // Reward settings
    private boolean enableItemRewards;
    private boolean enablePokemonRewards;
    private boolean enableCommandRewards;
    
    public PokedexConfig() {
        setDefaults();
    }
    
    private void setDefaults() {
        this.maxTiers = 10;
        this.totalPokemon = 714; // Default to Gen 9 total
        this.enablePermissionNodes = true;
        this.savePlayerData = true;
        this.dataVersion = "2.0"; // Updated for enhanced features
        
        // Enhanced configuration defaults
        this.messagePrefix = "§b[§dSimpleDexRewards§b]§r ";
        this.enableShinyTracking = true;
        this.enableLivingDexTracking = false;
        this.totalShinyPokemon = 714; // Same as totalPokemon by default
        
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
        this.totalPokemon = getOrDefault(json, "totalPokemon", 714);
        this.enablePermissionNodes = getOrDefault(json, "enablePermissionNodes", true);
        this.savePlayerData = getOrDefault(json, "savePlayerData", true);
        this.dataVersion = getOrDefault(json, "dataVersion", "2.0");
        
        // Enhanced configuration loading with validation
        this.messagePrefix = validatePrefix(getOrDefault(json, "messagePrefix", "§b[§dSimpleDexRewards§b]§r "));
        this.enableShinyTracking = getOrDefault(json, "enableShinyTracking", true);
        this.enableLivingDexTracking = getOrDefault(json, "enableLivingDexTracking", false);
        this.totalShinyPokemon = Math.max(1, getOrDefault(json, "totalShinyPokemon", 714));
        
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
    
    /**
     * Validates and sanitizes the message prefix configuration
     * @param prefix The prefix to validate
     * @return A valid prefix string
     */
    private String validatePrefix(String prefix) {
        if (prefix == null || prefix.trim().isEmpty()) {
            CobblemonPokedex.LOGGER.warn("Invalid message prefix configured, using default");
            return "§b[§dSimpleDexRewards§b]§r ";
        }
        
        // Ensure prefix ends with a space for proper formatting
        if (!prefix.endsWith(" ")) {
            prefix += " ";
        }
        
        return prefix;
    }
    
    public void save() {
        JsonObject json = new JsonObject();
        json.addProperty("maxTiers", maxTiers);
        json.addProperty("totalPokemon", totalPokemon);
        json.addProperty("enablePermissionNodes", enablePermissionNodes);
        json.addProperty("savePlayerData", savePlayerData);
        json.addProperty("dataVersion", dataVersion);
        
        // Enhanced configuration saving
        json.addProperty("messagePrefix", messagePrefix);
        json.addProperty("enableShinyTracking", enableShinyTracking);
        json.addProperty("enableLivingDexTracking", enableLivingDexTracking);
        json.addProperty("totalShinyPokemon", totalShinyPokemon);
        
        json.addProperty("enableItemRewards", enableItemRewards);
        json.addProperty("enablePokemonRewards", enablePokemonRewards);
        json.addProperty("enableCommandRewards", enableCommandRewards);
        
        Utils.writeFileSync(CONFIG_PATH, CONFIG_FILE, Utils.newGson().toJson(json));
    }
    
    // Getters
    public int getMaxTiers() { return maxTiers; }
    public int getTotalPokemon() { return totalPokemon; }
    public boolean isEnablePermissionNodes() { return enablePermissionNodes; }
    public boolean isSavePlayerData() { return savePlayerData; }
    public String getDataVersion() { return dataVersion; }
    
    // Enhanced configuration getters
    public String getMessagePrefix() { return messagePrefix; }
    public boolean isEnableShinyTracking() { return enableShinyTracking; }
    public boolean isEnableLivingDexTracking() { return enableLivingDexTracking; }
    public int getTotalShinyPokemon() { return totalShinyPokemon; }
    
    public boolean isEnableItemRewards() { return enableItemRewards; }
    public boolean isEnablePokemonRewards() { return enablePokemonRewards; }
    public boolean isEnableCommandRewards() { return enableCommandRewards; }
}

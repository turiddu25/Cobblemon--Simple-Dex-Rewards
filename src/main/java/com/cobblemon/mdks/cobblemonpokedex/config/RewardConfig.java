package com.cobblemon.mdks.cobblemonpokedex.config;

import com.cobblemon.mdks.cobblemonpokedex.CobblemonPokedex;
import com.cobblemon.mdks.cobblemonpokedex.util.Utils;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.BuiltInRegistries;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RewardConfig {
    private static final String CONFIG_PATH = "config/cobblemonpokedex";
    private static final String CONFIG_FILE = "rewardconfig.json";
    
    private List<Integer> completionTiers;
    private Map<String, RewardTier> rewards;
    private boolean enablePermissionNodes;
    
    public RewardConfig() {
        this.rewards = new HashMap<>();
        Utils.checkForDirectory("/" + CONFIG_PATH);
        CobblemonPokedex.LOGGER.info("Loading reward configuration...");
        load();
    }
    
    public void load() {
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
            CobblemonPokedex.LOGGER.error("Failed to load reward config", e);
            setDefaults();
            save();
        }
    }
    
    private void loadFromJson(JsonObject json) {
        this.enablePermissionNodes = json.has("enablePermissionNodes") && 
                                   json.get("enablePermissionNodes").getAsBoolean();
        
        // Load completion tiers
        if (json.has("completionTiers")) {
            this.completionTiers = json.get("completionTiers").getAsJsonArray().asList()
                .stream()
                .map(JsonElement::getAsInt)
                .toList();
        }
        
        // Load rewards
        this.rewards.clear();
        if (json.has("rewards")) {
            JsonObject rewardsJson = json.getAsJsonObject("rewards");
            for (Map.Entry<String, JsonElement> entry : rewardsJson.entrySet()) {
                String key = entry.getKey();
                JsonObject rewardJson = entry.getValue().getAsJsonObject();
                RewardTier reward = new RewardTier(rewardJson);
                rewards.put(key, reward);
            }
        }
    }
    
    private void setDefaults() {
        this.enablePermissionNodes = true;
        this.completionTiers = List.of(10, 20, 30, 40, 50, 60, 70, 80, 90, 100);
        rewards.clear();

        // Define rewards with their display items
        Object[][] rewardData = {
            // command, display item, row, slot
            {"cobblemon:poke_ball 5", "minecraft:red_dye", 1, 2},
            {"cobblemon:great_ball 3", "minecraft:blue_dye", 1, 4},
            {"cobblemon:ultra_ball 2", "minecraft:yellow_dye", 1, 6},
            {"cobblemon:master_ball 1", "minecraft:purple_dye", 1, 8},
            {"cobblemon:exp_share 1", "minecraft:experience_bottle", 3, 2},
            {"cobblemon:rare_candy 3", "minecraft:diamond", 3, 4},
            {"cobblemon:exp_candy_l 5", "minecraft:emerald", 3, 6},
            {"cobblemon:exp_candy_xl 3", "minecraft:gold_ingot", 3, 8},
            {"cobblemon:master_ball 2", "minecraft:nether_star", 5, 2},
            {"cobblemon:master_ball 3", "minecraft:dragon_egg", 5, 4}
        };

        // Set up tier rewards
        for (int i = 0; i < completionTiers.size(); i++) {
            int tier = completionTiers.get(i);
            RewardTier reward = new RewardTier();
            reward.command = "give %player% " + rewardData[i][0];
            reward.row = (int)rewardData[i][2];
            reward.slot = (int)rewardData[i][3];
            
            DisplayInfo display = new DisplayInfo("tier_" + tier, "Tier " + tier + " Reward");
            display.item = (String)rewardData[i][1];
            reward.display = display;
            
            rewards.put(String.valueOf(tier), reward);
        }

        // Add completion display
        RewardTier completion = new RewardTier();
        completion.row = 6;
        completion.slot = 5;
        completion.display = new DisplayInfo("completion", "Caught: {caught}/{total} ({percent}%)");
        completion.display.item = "minecraft:experience_bottle";
        rewards.put("completion", completion);
    }
    
    public void save() {
        JsonObject json = new JsonObject();
        json.addProperty("enablePermissionNodes", enablePermissionNodes);
        
        // Save completion tiers
        var tiersArray = Utils.newGson().toJsonTree(completionTiers).getAsJsonArray();
        json.add("completionTiers", tiersArray);
        
        JsonObject rewardsJson = new JsonObject();
        for (Map.Entry<String, RewardTier> entry : rewards.entrySet()) {
            rewardsJson.add(entry.getKey(), entry.getValue().toJson());
        }
        json.add("rewards", rewardsJson);
        
        Utils.writeFileSync(CONFIG_PATH, CONFIG_FILE, Utils.newGson().toJson(json));
    }
    
    public RewardTier getReward(String tier) {
        return rewards.get(tier);
    }
    
    public List<Integer> getCompletionTiers() {
        return completionTiers;
    }
    
    public boolean isEnablePermissionNodes() {
        return enablePermissionNodes;
    }
    
    public static class RewardTier {
        private int row;
        private int slot;
        private String command;
        private DisplayInfo display;
        
        public RewardTier() {}
        
        public RewardTier(JsonObject json) {
            this.row = json.has("row") ? json.get("row").getAsInt() : 1;
            this.slot = json.has("slot") ? json.get("slot").getAsInt() : 1;
            this.command = json.has("command") ? json.get("command").getAsString() : null;
            
            if (json.has("display")) {
                JsonObject displayJson = json.getAsJsonObject("display");
                String type = displayJson.get("type").getAsString();
                String format = displayJson.get("format").getAsString();
                this.display = new DisplayInfo(type, format);
                if (displayJson.has("item")) {
                    this.display.item = displayJson.get("item").getAsString();
                }
            }
        }
        
        public JsonObject toJson() {
            JsonObject json = new JsonObject();
            json.addProperty("row", row);
            json.addProperty("slot", slot);
            if (command != null) json.addProperty("command", command);
            if (display != null) json.add("display", display.toJson());
            return json;
        }
        
        public void grant(ServerPlayer player) {
            if (command != null && !command.isEmpty()) {
                String finalCommand = command
                    .replace("%player%", player.getName().getString())
                    .replace("%uuid%", player.getUUID().toString());
                
                CommandSourceStack source = player.getServer().createCommandSourceStack();
                player.getServer().getCommands().performPrefixedCommand(source, finalCommand);
            }
        }

        public ItemStack getDisplayItem() {
            if (display == null) {
                CobblemonPokedex.LOGGER.error("Display info is null for reward");
                return new ItemStack(Items.PAPER);
            }
            if (display.getItem() == null) {
                CobblemonPokedex.LOGGER.error("Display item is null for reward type: " + display.getType());
                return new ItemStack(Items.PAPER);
            }

            ResourceLocation itemId = ResourceLocation.tryParse(display.getItem());
            if (itemId == null) {
                CobblemonPokedex.LOGGER.error("Failed to parse item ID: " + display.getItem());
                return new ItemStack(Items.PAPER);
            }

            if (!BuiltInRegistries.ITEM.containsKey(itemId)) {
                CobblemonPokedex.LOGGER.error("Item not found in registry: " + itemId);
                return new ItemStack(Items.PAPER);
            }

            ItemStack stack = new ItemStack(BuiltInRegistries.ITEM.get(itemId));
            CobblemonPokedex.LOGGER.info("Created display item: " + display.getItem() + " -> " + stack.getItem().getName(stack).getString());
            return stack;
        }
        
        public int getRow() { return row; }
        public int getSlot() { return slot; }
        public String getCommand() { return command; }
        public DisplayInfo getDisplay() { return display; }
    }
    
    public static class DisplayInfo {
        private String type;
        private String format;
        private String item;
        
        public DisplayInfo(String type, String format) {
            this.type = type;
            this.format = format;
            this.item = null;
        }
        
        public JsonObject toJson() {
            JsonObject json = new JsonObject();
            json.addProperty("type", type);
            json.addProperty("format", format);
            if (item != null) json.addProperty("item", item);
            return json;
        }
        
        public String getType() { return type; }
        public String getFormat() { return format; }
        public String getItem() { return item; }
    }
}

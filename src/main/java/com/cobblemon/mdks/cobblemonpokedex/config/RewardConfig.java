package com.cobblemon.mdks.cobblemonpokedex.config;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.cobblemon.mdks.cobblemonpokedex.CobblemonPokedex;
import com.cobblemon.mdks.cobblemonpokedex.util.Utils;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class RewardConfig {
    private static final String CONFIG_PATH = "config/simpledexrewards";
    private static final String CONFIG_FILE = "rewardconfig.json";
    
    private List<Integer> completionTiers;
    private Map<String, RewardTier> rewards;
    private boolean enablePermissionNodes;
    private String congratulatoryMessageTemplate;
    
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
        
        // Load congratulatory message template
        this.congratulatoryMessageTemplate = json.has("congratulatoryMessageTemplate") 
            ? json.get("congratulatoryMessageTemplate").getAsString() 
            : "[SimpleDexRewards] §a§lCongratulations! §aYou received your reward for reaching §e{tier}%§a completion!";
        
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
        this.congratulatoryMessageTemplate = "[SimpleDexRewards] §a§lCongratulations! §aYou received your reward for reaching §e{tier}%§a completion!";

        setupDefaultReward(10, 1, 2, new Reward[]{
            createItemReward("minecraft:diamond", 3),
            createPokemonReward("Charmander", false, 5),
            createCommandReward("effect give @p minecraft:speed 60 1", "minecraft:paper", "Speed Boost")
        }, "cobblemon:poke_ball");

        setupDefaultReward(20, 1, 4, new Reward[]{
            createItemReward("minecraft:emerald", 5),
            createPokemonReward("Bulbasaur", true, 10),
            createCommandReward("effect give @p minecraft:jump_boost 60 1", "minecraft:paper", "Jump Boost")
        }, "cobblemon:citrine_ball");

        setupDefaultReward(30, 1, 6, new Reward[]{
            createItemReward("minecraft:gold_ingot", 7),
            createPokemonReward("Squirtle", false, 15),
            createCommandReward("effect give @p minecraft:night_vision 60 1", "minecraft:paper", "Night Vision")
        }, "cobblemon:verdant_ball");

        setupDefaultReward(40, 1, 8, new Reward[]{
            createItemReward("minecraft:iron_ingot", 10),
            createPokemonReward("Pikachu", true, 20),
            createCommandReward("effect give @p minecraft:strength 60 1", "minecraft:paper", "Strength Boost")
        }, "cobblemon:azure_ball");

        setupDefaultReward(50, 3, 2, new Reward[]{
            createItemReward("minecraft:netherite_scrap", 5),
            createPokemonReward("Eevee", false, 25),
            createCommandReward("effect give @p minecraft:invisibility 60 1", "minecraft:paper", "Invisibility")
        }, "cobblemon:roseate_ball");

        setupDefaultReward(60, 3, 4, new Reward[]{
            createItemReward("minecraft:lapis_lazuli", 8),
            createPokemonReward("Jigglypuff", true, 30),
            createCommandReward("effect give @p minecraft:regeneration 60 1", "minecraft:paper", "Regeneration")
        }, "cobblemon:slate_ball");

        setupDefaultReward(70, 3, 6, new Reward[]{
            createItemReward("minecraft:redstone", 12),
            createPokemonReward("Snorlax", false, 35),
            createCommandReward("effect give @p minecraft:fire_resistance 60 1", "minecraft:paper", "Fire Resistance")
        }, "cobblemon:great_ball");

        setupDefaultReward(80, 3, 8, new Reward[]{
            createItemReward("minecraft:coal", 15),
            createPokemonReward("Mewtwo", true, 40),
            createCommandReward("effect give @p minecraft:levitation 10 1", "minecraft:paper", "Levitation")
        }, "cobblemon:premier_ball");

        setupDefaultReward(90, 5, 3, new Reward[]{
            createItemReward("minecraft:quartz", 20),
            createPokemonReward("Arcanine", false, 45),
            createCommandReward("effect give @p minecraft:instant_health 1 1", "minecraft:paper", "Instant Health")
        }, "cobblemon:ultra_ball");

        setupDefaultReward(100, 5, 7, new Reward[]{
            createItemReward("minecraft:nether_star", 1),
            createPokemonReward("Rayquaza", true, 50),
            createCommandReward("give @p minecraft:totem_of_undying 1", "minecraft:paper", "Totem of Undying")
        }, "cobblemon:master_ball");
    }

    private void setupDefaultReward(int tier, int row, int slot, Reward[] rewards, String displayItem) {
        RewardTier rewardTier = new RewardTier();
        rewardTier.row = row;
        rewardTier.slot = slot;
        rewardTier.rewards = List.of(rewards);
        String displayName = switch(tier) {
            case 100 -> "Master Trainer";
            case 90 -> "Elite Trainer";
            case 80 -> "Expert Trainer";
            case 70 -> "Veteran Trainer";
            case 60 -> "Skilled Trainer";
            case 50 -> "Advanced Trainer";
            case 40 -> "Intermediate Trainer";
            case 30 -> "Developing Trainer";
            case 20 -> "Novice Trainer";
            case 10 -> "Beginner Trainer";
            default -> tier + "% Completion";
        };
        rewardTier.display = new DisplayInfo("tier_" + tier, displayName);
        rewardTier.display.item = displayItem;
        this.rewards.put(String.valueOf(tier), rewardTier);
    }

    private Reward createItemReward(String itemId, int count) {
        JsonObject data = new JsonObject();
        data.addProperty("id", itemId);
        data.addProperty("Count", count);
        return Reward.item(data);
    }

    private Reward createPokemonReward(String species, boolean shiny, int level) {
        JsonObject data = new JsonObject();
        data.addProperty("species", species);
        data.addProperty("shiny", shiny);
        data.addProperty("level", level);
        return Reward.pokemon(data);
    }

    private Reward createCommandReward(String command, String displayId, String displayName) {
        return Reward.command(command, displayId, displayName);
    }
    
    public void save() {
        JsonObject json = new JsonObject();
        json.addProperty("enablePermissionNodes", enablePermissionNodes);
        
        // Save completion tiers
        var tiersArray = Utils.newGson().toJsonTree(completionTiers).getAsJsonArray();
        json.add("completionTiers", tiersArray);
        
        // Save congratulatory message template
        json.addProperty("congratulatoryMessageTemplate", congratulatoryMessageTemplate);
        
        JsonObject rewardsJson = new JsonObject();
        for (Map.Entry<String, RewardTier> entry : rewards.entrySet()) {
            rewardsJson.add(entry.getKey(), entry.getValue().toJson());
        }
        json.add("rewards", rewardsJson);
        
        Utils.writeFileSync(CONFIG_PATH, CONFIG_FILE, Utils.newGson().toJson(json));
    }
    
    public RewardTier getReward(String tier) {
        if ("completion".equals(tier)) {
            // Return hardcoded completion tracker
            RewardTier completion = new RewardTier();
            completion.row = 6;
            completion.slot = 5;
            completion.display = new DisplayInfo("completion", "Caught: {caught}/{total} ({percent}%)");
            completion.display.item = "minecraft:experience_bottle";
            return completion;
        }
        return rewards.get(tier);
    }
    
    public List<Integer> getCompletionTiers() {
        return completionTiers;
    }
    
    public boolean isEnablePermissionNodes() {
        return enablePermissionNodes;
    }
    
    public String getCongratulatoryMessageTemplate() {
        return congratulatoryMessageTemplate;
    }
    
    public static class RewardTier {
        private int row;
        private int slot;
        private List<Reward> rewards;
        private DisplayInfo display;
        
        public RewardTier() {
            this.rewards = new ArrayList<>();
        }
        
        public RewardTier(JsonObject json) {
            this.row = json.has("row") ? json.get("row").getAsInt() : 1;
            this.slot = json.has("slot") ? json.get("slot").getAsInt() : 1;
            
            this.rewards = new ArrayList<>();
            if (json.has("rewards")) {
                JsonArray rewardsArray = json.getAsJsonArray("rewards");
                for (JsonElement element : rewardsArray) {
                    rewards.add(Reward.fromJson(element.getAsJsonObject()));
                }
            } else if (json.has("command")) {
                // Legacy support for old config format
                String command = json.get("command").getAsString();
                JsonObject data = new JsonObject();
                data.addProperty("id", "minecraft:paper");
                data.addProperty("display_name", "Legacy Reward");
                rewards.add(Reward.command(command, "minecraft:paper", "Legacy Reward"));
            }
            
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
            
            JsonArray rewardsArray = new JsonArray();
            for (Reward reward : rewards) {
                rewardsArray.add(reward.toJson());
            }
            json.add("rewards", rewardsArray);
            
            if (display != null) {
                json.add("display", display.toJson());
            }
            return json;
        }
        
        public void grant(ServerPlayer player) {
            for (Reward reward : rewards) {
                reward.grant(player);
            }
        }

        public ItemStack getDisplayItem() {
            if (display == null || display.getItem() == null) {
                CobblemonPokedex.LOGGER.error("Display info or item is null for reward");
                return new ItemStack(Items.PAPER);
            }

            ResourceLocation itemId = ResourceLocation.tryParse(display.getItem());
            if (itemId == null || !BuiltInRegistries.ITEM.containsKey(itemId)) {
                CobblemonPokedex.LOGGER.error("Invalid item ID: " + display.getItem());
                return new ItemStack(Items.PAPER);
            }

            ItemStack stack = new ItemStack(BuiltInRegistries.ITEM.get(itemId));
            return stack;
        }

        public String getHoverText() {
            StringBuilder text = new StringBuilder(display.getFormat());
            text.append("\n\nRewards:");
            for (Reward reward : rewards) {
                text.append("\n• ").append(reward.getDisplayText());
            }
            return text.toString();
        }
        
        public int getRow() { return row; }
        public int getSlot() { return slot; }
        public List<Reward> getRewards() { return rewards; }
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

package com.cobblemon.mdks.cobblemonpokedex.config;

import com.cobblemon.mdks.cobblemonpokedex.CobblemonPokedex;
import com.google.gson.JsonObject;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

public class Reward {
    private final RewardType type;
    private final JsonObject data;
    private final String command;

    public Reward(RewardType type, JsonObject data, String command) {
        this.type = type;
        this.data = data;
        this.command = command;
    }

    public RewardType getType() {
        return type;
    }

    public JsonObject getData() {
        return data;
    }

    public String getCommand() {
        return command;
    }

    public ItemStack getItemStack(RegistryAccess registryAccess) {
        if (type != RewardType.ITEM) {
            return ItemStack.EMPTY;
        }

        try {
            CompoundTag tag = TagParser.parseTag(data.toString());
            return ItemStack.parse(registryAccess, tag).orElse(ItemStack.EMPTY);
        } catch (Exception e) {
            CobblemonPokedex.LOGGER.error("Failed to parse item data", e);
            return ItemStack.EMPTY;
        }
    }

    public String getDisplayText() {
        switch (type) {
            case ITEM:
                try {
                    CompoundTag tag = TagParser.parseTag(data.toString());
                    String itemId = tag.getString("id");
                    int count = tag.contains("Count") ? tag.getInt("Count") : 1;
                    return count + "x " + itemId.split(":")[1].replace("_", " ");
                } catch (Exception e) {
                    return "Unknown Item";
                }
            case POKEMON:
                try {
                    String species = data.get("species").getAsString();
                    boolean shiny = data.has("shiny") && data.get("shiny").getAsBoolean();
                    return (shiny ? "Shiny " : "") + species;
                } catch (Exception e) {
                    return "Unknown Pokemon";
                }
            case COMMAND:
                return data.has("display_name") ? data.get("display_name").getAsString() : "Custom Reward";
            default:
                return "Unknown Reward";
        }
    }

    public void grant(ServerPlayer player) {
        switch (type) {
            case ITEM:
                ItemStack item = getItemStack(player.level().registryAccess());
                if (!item.isEmpty()) {
                    player.getInventory().add(item);
                }
                break;
            case POKEMON:
                if (data != null && !data.isEmpty()) {
                    try {
                        String species = data.get("species").getAsString();
                        StringBuilder cmd = new StringBuilder();
                        cmd.append("givepokemonother ").append(player.getName().getString()).append(" ").append(species);
                        
                        if (data.has("shiny") && data.get("shiny").getAsBoolean()) {
                            cmd.append(" shiny");
                        }
                        if (data.has("level")) {
                            cmd.append(" level=").append(data.get("level").getAsInt());
                        }
                        if (data.has("ability")) {
                            cmd.append(" ability=").append(data.get("ability").getAsString());
                        }
                        
                        CommandSourceStack source = player.getServer().createCommandSourceStack();
                        player.getServer().getCommands().performPrefixedCommand(source, cmd.toString());
                    } catch (Exception e) {
                        CobblemonPokedex.LOGGER.error("Failed to grant Pokemon reward", e);
                    }
                }
                break;
            case COMMAND:
                if (command != null && !command.isEmpty()) {
                    String finalCommand = command
                        .replace("%player%", player.getName().getString())
                        .replace("%uuid%", player.getUUID().toString());
                    
                    CommandSourceStack source = player.getServer().createCommandSourceStack();
                    player.getServer().getCommands().performPrefixedCommand(source, finalCommand);
                }
                break;
        }
    }

    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        json.addProperty("type", type.name());
        json.add("data", data);
        if (type == RewardType.COMMAND && command != null) {
            json.addProperty("command", command);
        }
        return json;
    }

    public static Reward fromJson(JsonObject json) {
        RewardType type = RewardType.fromString(json.get("type").getAsString());
        JsonObject data = json.get("data").getAsJsonObject();
        String command = json.has("command") ? json.get("command").getAsString() : null;
        return new Reward(type, data, command);
    }

    // Factory methods for different reward types
    public static Reward item(JsonObject nbtData) {
        return new Reward(RewardType.ITEM, nbtData, null);
    }

    public static Reward pokemon(JsonObject pokemonData) {
        return new Reward(RewardType.POKEMON, pokemonData, null);
    }

    public static Reward command(String commandData, String displayId, String displayName) {
        JsonObject data = new JsonObject();
        data.addProperty("id", displayId);
        data.addProperty("display_name", displayName);
        return new Reward(RewardType.COMMAND, data, commandData);
    }
}

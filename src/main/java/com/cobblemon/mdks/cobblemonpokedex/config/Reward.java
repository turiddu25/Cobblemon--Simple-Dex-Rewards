package com.cobblemon.mdks.cobblemonpokedex.config;

import java.util.List;

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
    private final List<CommandEntry> commands;

    public Reward(RewardType type, JsonObject data, List<CommandEntry> commands) {
        this.type = type;
        this.data = data;
        this.commands = commands;
    }


    public RewardType getType() {
        return type;
    }

    public JsonObject getData() {
        return data;
    }

    public List<CommandEntry> getCommands() {
        return commands;
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
                if (commands != null && !commands.isEmpty()) {
                    for (CommandEntry entry : commands) {
                        String finalCommand = entry.getCommand()
                            .replace("%player%", player.getName().getString())
                            .replace("%uuid%", player.getUUID().toString());
                        
                        CommandSourceStack source = player.getServer().createCommandSourceStack();
                        player.getServer().getCommands().performPrefixedCommand(source, finalCommand);
                    }
                }
                break;
        }
    }

    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        json.addProperty("type", type.name());
        json.add("data", data);
        if (type == RewardType.COMMAND && commands != null && !commands.isEmpty()) {
            com.google.gson.JsonArray array = new com.google.gson.JsonArray();
            for (CommandEntry entry : commands) {
                array.add(entry.toJson());
            }
            json.add("commands", array);
        }
        return json;
    }

    public static Reward fromJson(JsonObject json) {
        RewardType type = RewardType.fromString(json.get("type").getAsString());
        JsonObject data = json.get("data").getAsJsonObject();
        java.util.List<CommandEntry> commandList = java.util.Collections.emptyList();
        if (type == RewardType.COMMAND) {
            if (json.has("commands")) {
                com.google.gson.JsonArray arr = json.getAsJsonArray("commands");
                java.util.List<CommandEntry> list = new java.util.ArrayList<>();
                for (com.google.gson.JsonElement el : arr) {
                    list.add(CommandEntry.fromJson(el.getAsJsonObject()));
                }
                commandList = list;
            } else if (json.has("command")) {
                commandList = java.util.List.of(new CommandEntry(json.get("command").getAsString(), false));
            }
        }
        return new Reward(type, data, commandList);
    }

    // Factory methods for different reward types
    public static Reward item(JsonObject nbtData) {
        return new Reward(RewardType.ITEM, nbtData, java.util.Collections.emptyList());
    }

    public static Reward pokemon(JsonObject pokemonData) {
        return new Reward(RewardType.POKEMON, pokemonData, java.util.Collections.emptyList());
    }

    public static Reward command(String commandData, String displayId, String displayName) {
        JsonObject data = new JsonObject();
        data.addProperty("id", displayId);
        data.addProperty("display_name", displayName);
        return new Reward(RewardType.COMMAND, data, java.util.List.of(new CommandEntry(commandData, true)));
    }
    public static class CommandEntry {
        private final String command;
        private final boolean hidden;
        
        public CommandEntry(String command, boolean hidden) {
            this.command = command;
            this.hidden = hidden;
        }
        
        public String getCommand() {
            return command;
        }
        
        public boolean isHidden() {
            return hidden;
        }
        
        public JsonObject toJson() {
            JsonObject json = new JsonObject();
            json.addProperty("command", command);
            json.addProperty("hidden", hidden);
            return json;
        }
        
        public static CommandEntry fromJson(JsonObject json) {
            String command = json.get("command").getAsString();
            boolean hidden = json.has("hidden") ? json.get("hidden").getAsBoolean() : false;
            return new CommandEntry(command, hidden);
        }
    }
}

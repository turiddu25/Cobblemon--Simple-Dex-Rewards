package com.cobblemon.mdks.cobblemonpokedex.command;

import com.cobblemon.mdks.cobblemonpokedex.CobblemonPokedex;
import com.cobblemon.mdks.cobblemonpokedex.config.Config;
import com.cobblemon.mdks.cobblemonpokedex.util.*;
import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.api.pokemon.PokemonSpecies;
import com.cobblemon.mod.common.api.pokedex.*;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import ca.landonjw.gooeylibs2.api.UIManager;
import ca.landonjw.gooeylibs2.api.button.GooeyButton;
import ca.landonjw.gooeylibs2.api.button.PlaceholderButton;
import ca.landonjw.gooeylibs2.api.page.LinkedPage;
import ca.landonjw.gooeylibs2.api.template.types.ChestTemplate;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.core.component.DataComponents;
import kotlin.Unit;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.network.chat.Component;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class DexRewardsCommand extends BaseCommand {
    public DexRewardsCommand() {
        super("dexrewards", 
              Collections.emptyList(), // no aliases
              null, // no permission required
              Collections.emptyList() // no subcommands
        );
    }

    @Override
    public int run(CommandContext<CommandSourceStack> context) {
        ServerPlayer player = context.getSource().getPlayer();
        if (player == null) return 0;

        // Get player's Pokedex completion percentage
        double completionPercentage = getPokedexCompletion(player);
        
        // Open the Pokedex progression UI
        openPokedexUI(player, completionPercentage);

        return 1;
    }

    private double getPokedexCompletion(ServerPlayer player) {
        // Get player's Pokedex instance
        var pokedex = Cobblemon.INSTANCE.getPlayerDataManager().getPokedexData(player);
        
        // Get total number of Pokemon species
        int totalSpecies = PokemonSpecies.INSTANCE.count();
        
        // Get number of caught Pokemon
        int caughtCount = pokedex.getDexCalculatedValue(
            ResourceLocation.tryParse("cobblemon:national"),
            CaughtCount.INSTANCE
        );
        
        // Calculate completion percentage
        return (double) caughtCount / totalSpecies * 100;
    }

    private void openPokedexUI(ServerPlayer player, double completionPercentage) {
        Config config = CobblemonPokedex.config;
        
        // Create main page template
        ChestTemplate template = ChestTemplate.builder(6)
            .fill(new PlaceholderButton()) // Fill with empty background
            .build();

        // Add title button
        GooeyButton titleButton = GooeyButton.builder()
            .display(new ItemStack(Items.PAPER))
            .with(DataComponents.CUSTOM_NAME, Component.literal(String.format("Completion: %.1f%%", completionPercentage)))
            .with(DataComponents.HIDE_ADDITIONAL_TOOLTIP, net.minecraft.util.Unit.INSTANCE)
            .build();
        template.set(0, 4, titleButton);

        // Create page
        LinkedPage page = LinkedPage.builder()
            .template(template)
            .title("Pokedex Progression")
            .build();
        
        // Add tier buttons
        int slot = 1;
        for (int tier : config.getCompletionTiers()) {
            boolean completed = completionPercentage >= tier;
            
            ItemStack displayItem = getRewardItemForTier(tier, player);
            List<Component> lore = new ArrayList<>();
            lore.add(Component.literal("§7Rewards:"));
            if (completed) {
                lore.add(Component.literal("§a✔ Tier Complete!"));
            } else {
                lore.add(Component.literal("§c✘ Not Complete"));
            }

            GooeyButton tierButton = GooeyButton.builder()
                .display(displayItem)
                .with(DataComponents.CUSTOM_NAME, Component.literal("§6" + tier + "% Completion"))
                .with(DataComponents.LORE, new ItemLore(lore))
                .with(DataComponents.HIDE_ADDITIONAL_TOOLTIP, net.minecraft.util.Unit.INSTANCE)
                .onClick(action -> claimRewards(player, tier))
                .build();
            
            template.set(slot++, 0, tierButton);
        }
        
        UIManager.openUIForcefully(player, page);
    }

    private ItemStack getRewardItemForTier(int tier, ServerPlayer player) {
        JsonElement reward = CobblemonPokedex.config.getRewardForTier(tier);
        if (reward == null || !reward.isJsonObject()) {
            return new ItemStack(Items.DIAMOND);
        }
        
        try {
            JsonObject rewardObj = reward.getAsJsonObject();
            String itemId = rewardObj.get("id").getAsString();
            int count = rewardObj.has("Count") ? rewardObj.get("Count").getAsInt() : 1;
            
            return new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse("minecraft:" + itemId)), count);
        } catch (Exception e) {
            return new ItemStack(Items.DIAMOND);
        }
    }

    private void claimRewards(ServerPlayer player, int tier) {
        double completionPercentage = getPokedexCompletion(player);
        if (completionPercentage < tier) {
            player.sendSystemMessage(Component.literal("§cYou haven't reached " + tier + "% completion yet!"));
            return;
        }

        // Check if reward already claimed
        var playerData = CobblemonPokedex.playerDataConfig.getPlayerData(player.getUUID());
        if (playerData.hasClaimedReward(tier)) {
            player.sendSystemMessage(Component.literal("§cYou've already claimed this reward!"));
            return;
        }

        JsonElement reward = CobblemonPokedex.config.getRewardForTier(tier);
        if (reward == null || !reward.isJsonObject()) {
            player.sendSystemMessage(Component.literal("§cNo reward available for this tier!"));
            return;
        }

        try {
            JsonObject rewardObj = reward.getAsJsonObject();
            String itemId = rewardObj.get("id").getAsString();
            int count = rewardObj.has("Count") ? rewardObj.get("Count").getAsInt() : 1;
            
            ItemStack rewardItem = new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse("minecraft:" + itemId)), count);
            player.getInventory().add(rewardItem);
            
            // Mark reward as claimed
            playerData.setClaimedReward(tier, true);
            CobblemonPokedex.playerDataConfig.save();
            
            player.sendSystemMessage(Component.literal("§aYou received your reward for reaching " + tier + "% completion!"));
        } catch (Exception e) {
            player.sendSystemMessage(Component.literal("§cError giving reward: " + e.getMessage()));
            CobblemonPokedex.LOGGER.error("Error giving reward for tier " + tier, e);
        }
    }
}

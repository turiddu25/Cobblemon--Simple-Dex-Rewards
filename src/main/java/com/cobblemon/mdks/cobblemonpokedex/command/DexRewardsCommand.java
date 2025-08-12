package com.cobblemon.mdks.cobblemonpokedex.command;

import com.cobblemon.mdks.cobblemonpokedex.CobblemonPokedex;
import com.cobblemon.mdks.cobblemonpokedex.config.RewardConfig;
import com.cobblemon.mdks.cobblemonpokedex.config.RewardConfig.RewardTier;
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
    private static final org.slf4j.Logger LOGGER = CobblemonPokedex.LOGGER;
    private static final String PREFIX = "§b[§dSimpleDexRewards§b]§r ";

    public DexRewardsCommand() {
        super("dexrewards", 
              Collections.emptyList(), // no aliases
              null, // no permission required
              List.of(new ReloadSubcommand()) // Add reload subcommand
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
        try {
            // Get player's Pokedex instance
            var pokedex = Cobblemon.INSTANCE.getPlayerDataManager().getPokedexData(player);
            
            // Get number of unique species caught
            int uniqueSpeciesCaught = pokedex.getDexCalculatedValue(
                ResourceLocation.tryParse("cobblemon:national"),
                CaughtCount.INSTANCE
            );
            
            // Calculate completion percentage based on configured total Pokemon
            return (double) uniqueSpeciesCaught / CobblemonPokedex.pokedexConfig.getTotalPokemon() * 100;
        } catch (Exception e) {
            LOGGER.error("Error calculating Pokedex completion for player " + player.getName().getString(), e);
            return 0.0;
        }
    }

    private void openPokedexUI(ServerPlayer player, double completionPercentage) {
        // Create main page template with 6 rows and 9 columns
        ChestTemplate template = ChestTemplate.builder(6)
            .fill(new PlaceholderButton()) // Fill with empty background
            .build();

        // Add completion display
        int totalSpecies = CobblemonPokedex.pokedexConfig.getTotalPokemon();
        int uniqueSpeciesCaught = Cobblemon.INSTANCE.getPlayerDataManager()
            .getPokedexData(player)
            .getDexCalculatedValue(
                ResourceLocation.tryParse("cobblemon:national"),
                CaughtCount.INSTANCE
            );

        RewardTier completionReward = CobblemonPokedex.rewardConfig.getReward("completion");
        if (completionReward != null && completionReward.getDisplay() != null) {
            String format = completionReward.getDisplay().getFormat()
                .replace("{caught}", String.valueOf(uniqueSpeciesCaught))
                .replace("{total}", String.valueOf(totalSpecies))
                .replace("{percent}", String.format("%.1f", completionPercentage));

            GooeyButton completionButton = GooeyButton.builder()
                .display(completionReward.getDisplayItem())
                .with(DataComponents.CUSTOM_NAME, Component.literal("§6Pokedex Completion"))
                .with(DataComponents.LORE, new ItemLore(List.of(Component.literal("§7" + format))))
                .with(DataComponents.HIDE_ADDITIONAL_TOOLTIP, net.minecraft.util.Unit.INSTANCE)
                .build();
            template.set(completionReward.getRow() - 1, completionReward.getSlot() - 1, completionButton);
        }

        // Add reward buttons
        for (int tier : CobblemonPokedex.rewardConfig.getCompletionTiers()) {
            String tierKey = String.valueOf(tier);
            RewardTier reward = CobblemonPokedex.rewardConfig.getReward(tierKey);
            if (reward == null) {
                LOGGER.error("No reward found for tier: " + tierKey);
                continue;
            }
            LOGGER.info("Adding button for tier " + tierKey + " at row " + reward.getRow() + ", slot " + reward.getSlot());

            boolean completed = completionPercentage >= tier;
            boolean claimed = CobblemonPokedex.playerDataConfig
                .getPlayerData(player.getUUID())
                .hasClaimedReward(tier);

            List<Component> lore = new ArrayList<>();
            lore.add(Component.literal("§7Required: §f" + tier + "%"));

            // Add reward list to lore
            String[] rewardTexts = reward.getHoverText().split("\n");
            for (String text : rewardTexts) {
                if (text.startsWith("•")) {
                    lore.add(Component.literal("§7" + text));
                }
            }

            lore.add(Component.literal("")); // Empty line for spacing
            if (completed && claimed) {
                lore.add(Component.literal("§a✔ Claimed"));
            } else if (completed) {
                lore.add(Component.literal("§e⚡ Available"));
                lore.add(Component.literal("§7Click to claim!"));
            } else {
                lore.add(Component.literal("§c✘ Locked"));
                lore.add(Component.literal("§7Progress: §f" + String.format("%.1f%%", completionPercentage)));
            }

            // Get display item from reward tier
            ItemStack displayItem = reward.getDisplayItem();

            GooeyButton tierButton = GooeyButton.builder()
                .display(displayItem)
                .with(DataComponents.CUSTOM_NAME, Component.literal("§6" + reward.getDisplay().getFormat()))
                .with(DataComponents.LORE, new ItemLore(lore))
                .with(DataComponents.HIDE_ADDITIONAL_TOOLTIP, net.minecraft.util.Unit.INSTANCE)
                .onClick(action -> {
                    if (completed && !claimed) {
                        claimRewards(player, tier);
                        // Refresh UI with updated completion percentage
                        openPokedexUI(player, getPokedexCompletion(player));
                    } else if (!completed) {
                        player.sendSystemMessage(Component.literal(PREFIX + "§cYou need §e" + tier + "%§c completion to claim this reward! §7(Current: §f" + String.format("%.1f%%", completionPercentage) + "§7)"));
                    } else if (claimed) {
                        player.sendSystemMessage(Component.literal(PREFIX + "§cYou have already claimed this reward!"));
                    }
                })
                .build();
            
            template.set(reward.getRow() - 1, reward.getSlot() - 1, tierButton);
        }
        
        // Create and show page
        try {
            LinkedPage page = LinkedPage.builder()
                .template(template)
                .title("Pokedex Progression")
                .build();
            
            LOGGER.info("Opening UI with " + CobblemonPokedex.rewardConfig.getCompletionTiers().size() + " reward tiers");
            UIManager.openUIForcefully(player, page);
        } catch (Exception e) {
            CobblemonPokedex.LOGGER.error("Error opening Pokedex UI", e);
            player.sendSystemMessage(Component.literal(PREFIX + "§cError opening Pokedex UI. §7Please try again."));
        }
    }

    private void claimRewards(ServerPlayer player, int tier) {
        double completionPercentage = getPokedexCompletion(player);
        if (completionPercentage < tier) {
            player.sendSystemMessage(Component.literal(PREFIX + "§cYou need §e" + tier + "%§c completion to claim this reward! §7(Current: §f" + String.format("%.1f%%", completionPercentage) + "§7)"));
            return;
        }

        var playerData = CobblemonPokedex.playerDataConfig.getPlayerData(player.getUUID());
        if (playerData.hasClaimedReward(tier)) {
            player.sendSystemMessage(Component.literal(PREFIX + "§cYou have already claimed this reward!"));
            return;
        }

        RewardTier reward = CobblemonPokedex.rewardConfig.getReward(String.valueOf(tier));
        if (reward == null) {
            player.sendSystemMessage(Component.literal(PREFIX + "§cNo reward available for this tier!"));
            return;
        }

        try {
            // Grant the reward
            reward.grant(player);
            
            // Mark as claimed
            playerData.setClaimedReward(tier, true);
            CobblemonPokedex.playerDataConfig.savePlayer(player.getUUID());
            
            player.sendSystemMessage(Component.literal(PREFIX + CobblemonPokedex.rewardConfig.getCongratulatoryMessageTemplate().replace("{tier}", String.valueOf(tier))));
        } catch (Exception e) {
            player.sendSystemMessage(Component.literal(PREFIX + "§cError giving reward: §7" + e.getMessage()));
            CobblemonPokedex.LOGGER.error("Error giving reward for tier " + tier, e);
        }
    }
}

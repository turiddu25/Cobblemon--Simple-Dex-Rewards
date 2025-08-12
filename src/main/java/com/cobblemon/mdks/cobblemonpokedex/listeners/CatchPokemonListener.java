package com.cobblemon.mdks.cobblemonpokedex.listeners;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cobblemon.mdks.cobblemonpokedex.CobblemonPokedex;
import com.cobblemon.mdks.cobblemonpokedex.config.PlayerDataConfig;
import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.api.Priority;
import com.cobblemon.mod.common.api.events.CobblemonEvents;
import com.cobblemon.mod.common.api.events.pokemon.PokemonCapturedEvent;
import com.cobblemon.mod.common.api.pokedex.CaughtCount;

import kotlin.Unit;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

public class CatchPokemonListener {
    private static final Logger LOGGER = LoggerFactory.getLogger("cobblemonpokedex");
    private static final String PREFIX = "§b[§dSimpleDexRewards§b]§r ";

    public static void register() {
        CobblemonEvents.POKEMON_CAPTURED.subscribe(Priority.NORMAL, CatchPokemonListener::handle);
    }

    private static Unit handle(PokemonCapturedEvent event) {
        if (event.getPlayer() instanceof ServerPlayer player) {
            try {
                // Get player's Pokedex data
                var pokedex = Cobblemon.INSTANCE.getPlayerDataManager().getPokedexData(player);
                
                // Get number of unique species caught
                int uniqueSpeciesCaught = pokedex.getDexCalculatedValue(
                    ResourceLocation.tryParse("cobblemon:national"),
                    CaughtCount.INSTANCE
                );

                // Update player data with unique species count
                PlayerDataConfig.PlayerData playerData = CobblemonPokedex.playerDataConfig.getPlayerData(player.getUUID());
                playerData.updateTotalCaught(uniqueSpeciesCaught);
                
                // Save player data
                CobblemonPokedex.playerDataConfig.savePlayer(player.getUUID());
                
                // Calculate completion percentage based on total available Pokemon
                double completionPercentage = (double) uniqueSpeciesCaught / CobblemonPokedex.pokedexConfig.getTotalPokemon() * 100;
                
                // Check if we've just reached a new tier
                for (Integer tier : CobblemonPokedex.rewardConfig.getCompletionTiers()) {
                    if (!playerData.hasClaimedReward(tier) && 
                        completionPercentage >= tier && 
                        playerData.getTotalCaught() < uniqueSpeciesCaught) { // Only if we've just increased our count
                        player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                            PREFIX + "§a§lMilestone Reached! §aYou've achieved §e" + tier + "%§a Pokédex completion!\n" +
                            PREFIX + "§7Use §f/dexrewards§7 to claim your special rewards!"
                        ));
                        // Don't break - show messages for all reached tiers
                    }
                }
                
                LOGGER.debug("Updated unique species count for " + player.getName().getString() + ": " + uniqueSpeciesCaught);
            } catch (Exception e) {
                LOGGER.error("Error handling Pokemon capture for player " + player.getName().getString(), e);
            }
        }
        return Unit.INSTANCE;
    }
}

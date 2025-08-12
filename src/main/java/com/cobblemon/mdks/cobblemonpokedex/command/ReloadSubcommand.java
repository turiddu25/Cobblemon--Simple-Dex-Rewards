package com.cobblemon.mdks.cobblemonpokedex.command;

import com.cobblemon.mdks.cobblemonpokedex.CobblemonPokedex;
import com.cobblemon.mdks.cobblemonpokedex.config.PlayerDataConfig;
import com.cobblemon.mdks.cobblemonpokedex.util.Permissions;
import com.cobblemon.mdks.cobblemonpokedex.util.Subcommand;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.tree.CommandNode;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;

public class ReloadSubcommand extends Subcommand {
    public ReloadSubcommand() {
        super("§9Usage: §3/dexrewards reload");
    }

    @Override
    public int run(CommandContext<CommandSourceStack> context) {
        if (!context.getSource().hasPermission(4)) { // Requires operator permission level
            context.getSource().sendSystemMessage(
                Component.literal("§cYou don't have permission to use this command!")
            );
            return 0;
        }

        try {
            // Reload all configurations
            CobblemonPokedex.pokedexConfig.load();
            CobblemonPokedex.pokedexConfig.save(); // Save after load to ensure new fields are written
            context.getSource().sendSystemMessage(
                Component.literal("§aReloaded Pokedex configuration")
            );

            CobblemonPokedex.rewardConfig.load();
            context.getSource().sendSystemMessage(
                Component.literal("§aReloaded reward configuration")
            );

            CobblemonPokedex.playerDataConfig = new PlayerDataConfig();
            context.getSource().sendSystemMessage(
                Component.literal("§aReloaded player data")
            );

            CobblemonPokedex.permissions = new Permissions();
            context.getSource().sendSystemMessage(
                Component.literal("§aReloaded permissions")
            );

            context.getSource().sendSystemMessage(
                Component.literal("§a§lAll configurations reloaded successfully!")
            );
            return 1;
        } catch (Exception e) {
            context.getSource().sendSystemMessage(
                Component.literal("§cError reloading configurations: " + e.getMessage())
            );
            CobblemonPokedex.LOGGER.error("Failed to reload configurations", e);
            return 0;
        }
    }

    @Override
    public CommandNode<CommandSourceStack> build() {
        return LiteralArgumentBuilder.<CommandSourceStack>literal("reload")
            .requires(source -> source.hasPermission(4)) // Requires operator permission level
            .executes(this::run)
            .build();
    }
}

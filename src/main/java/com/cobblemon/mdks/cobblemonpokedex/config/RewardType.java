package com.cobblemon.mdks.cobblemonpokedex.config;

public enum RewardType {
    ITEM,             // Any mod's items (minecraft:, cobblemon:, etc)
    POKEMON,          // Pokemon rewards
    COMMAND;          // Custom command execution

    public static RewardType fromString(String type) {
        try {
            return valueOf(type.toUpperCase());
        } catch (IllegalArgumentException e) {
            return COMMAND; // Default to COMMAND type for backwards compatibility
        }
    }
}

package com.cobblemon.mdks.cobblemonpokedex.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.*;

public class PokedexConfigTest {
    
    private PokedexConfig config;
    
    @BeforeEach
    void setUp() {
        config = new PokedexConfig();
    }
    
    @Test
    void testDefaultValues() {
        assertEquals("§b[§dSimpleDexRewards§b]§r ", config.getMessagePrefix());
        assertTrue(config.isEnableShinyTracking());
        assertFalse(config.isEnableLivingDexTracking());
        assertEquals(714, config.getTotalShinyPokemon());
        assertEquals("2.0", config.getDataVersion());
    }
    
    @Test
    void testEnhancedConfigurationFields() {
        // Test that all new fields have appropriate getters
        assertNotNull(config.getMessagePrefix());
        assertNotNull(config.isEnableShinyTracking());
        assertNotNull(config.isEnableLivingDexTracking());
        assertTrue(config.getTotalShinyPokemon() > 0);
    }
    
    @Test
    void testDataVersionUpdate() {
        // Verify that the data version has been updated to 2.0 for enhanced features
        assertEquals("2.0", config.getDataVersion());
    }
    
    @Test
    void testShinyPokemonCountValidation() {
        // The total shiny Pokemon should be a positive number
        assertTrue(config.getTotalShinyPokemon() > 0);
        assertEquals(config.getTotalPokemon(), config.getTotalShinyPokemon());
    }
}
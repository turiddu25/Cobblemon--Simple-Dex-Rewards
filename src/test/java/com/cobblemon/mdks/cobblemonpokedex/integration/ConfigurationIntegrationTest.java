package com.cobblemon.mdks.cobblemonpokedex.integration;

import com.cobblemon.mdks.cobblemonpokedex.config.PokedexConfig;
import com.cobblemon.mdks.cobblemonpokedex.util.MessageHandler;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test to verify that the enhanced configuration infrastructure
 * works correctly with MessageHandler initialization.
 */
public class ConfigurationIntegrationTest {
    
    private PokedexConfig config;
    
    @BeforeEach
    void setUp() {
        config = new PokedexConfig();
    }
    
    @Test
    void testMessageHandlerIntegrationWithConfig() {
        // Test that MessageHandler can be initialized with config values
        String configPrefix = config.getMessagePrefix();
        MessageHandler.initialize(configPrefix);
        
        assertEquals(configPrefix, MessageHandler.getPrefix());
    }
    
    @Test
    void testEnhancedConfigurationIntegration() {
        // Verify all enhanced configuration fields are accessible
        assertNotNull(config.getMessagePrefix());
        assertNotNull(config.isEnableShinyTracking());
        assertNotNull(config.isEnableLivingDexTracking());
        assertTrue(config.getTotalShinyPokemon() > 0);
        
        // Test that the configuration maintains consistency
        assertEquals(config.getTotalPokemon(), config.getTotalShinyPokemon());
    }
    
    @Test
    void testConfigurationValidation() {
        // Test that configuration validation works properly
        assertTrue(config.getTotalShinyPokemon() >= 1, "Total shiny Pokemon should be at least 1");
        assertFalse(config.getMessagePrefix().trim().isEmpty(), "Message prefix should not be empty");
        assertEquals("2.0", config.getDataVersion(), "Data version should be updated to 2.0");
    }
}
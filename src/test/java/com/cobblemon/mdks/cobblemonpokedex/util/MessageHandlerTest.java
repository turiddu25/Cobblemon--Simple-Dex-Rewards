package com.cobblemon.mdks.cobblemonpokedex.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.*;

public class MessageHandlerTest {
    
    @BeforeEach
    void setUp() {
        // Reset to default before each test
        MessageHandler.initialize(null);
    }
    
    @Test
    void testInitializeWithDefaultPrefix() {
        MessageHandler.initialize(null);
        assertEquals("§b[§dSimpleDexRewards§b]§r ", MessageHandler.getPrefix());
    }
    
    @Test
    void testInitializeWithCustomPrefix() {
        String customPrefix = "§a[CustomMod]§r ";
        MessageHandler.initialize(customPrefix);
        assertEquals(customPrefix, MessageHandler.getPrefix());
    }
    
    @Test
    void testInitializeWithEmptyPrefix() {
        MessageHandler.initialize("");
        assertEquals("§b[§dSimpleDexRewards§b]§r ", MessageHandler.getPrefix());
    }
    
    @Test
    void testFormatMessage() {
        MessageHandler.initialize("§a[Test]§r ");
        String message = "Hello World";
        String expected = "§a[Test]§r Hello World";
        assertEquals(expected, MessageHandler.formatMessage(message).getString());
    }
    
    @Test
    void testPrefixValidation() {
        // Test that prefix without space gets space added
        MessageHandler.initialize("§a[Test]§r");
        assertEquals("§a[Test]§r ", MessageHandler.getPrefix());
    }
}
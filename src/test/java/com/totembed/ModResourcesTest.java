package com.totembed;

import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Validates the mod's data-driven wiring without a game bootstrap:
 * the enchantment JSON (the riskiest piece), mod metadata, mixin config
 * and translations. Runs in the plain {@code test} task on CI.
 */
public class ModResourcesTest {

    private static String resource(String path) {
        try (InputStream in = ModResourcesTest.class.getResourceAsStream(path)) {
            assertNotNull(in, "missing resource: " + path);
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (AssertionError e) {
            throw e;
        } catch (Exception e) {
            throw new AssertionError("cannot read resource: " + path, e);
        }
    }

    @Test
    public void enchantmentJsonBindsTotemOfUndying() {
        String json = resource("/data/totembed/enchantment/totem_stasis.json");
        assertTrue(json.contains("minecraft:totem_of_undying"),
            "supported_items must be the totem");
        assertTrue(json.contains("\"max_level\""), "max_level must be present");
        assertTrue(json.contains("\"hand\""), "slots must include hand");
        assertTrue(json.contains("enchantment.totembed.totem_stasis"),
            "description must point at our translation key");
    }

    @Test
    public void modMetadataWiresEntrypointsAndMixins() {
        String json = resource("/fabric.mod.json");
        assertTrue(json.contains("\"totembed\""), "mod id must be present");
        assertTrue(json.contains("TotemBedMod"), "main entrypoint must be present");
        assertTrue(json.contains("LivingEntityTotemMixin") || json.contains("totembed.mixins.json"),
            "mixin config must be wired");
        assertTrue(json.contains("1.21.1"), "minecraft target must be 1.21.1");
    }

    @Test
    public void mixinConfigTargetsTotemHook() {
        String json = resource("/totembed.mixins.json");
        assertTrue(json.contains("LivingEntityTotemMixin"), "totem mixin must be registered");
        assertTrue(json.contains("com.totembed.mixin"), "mixin package must match");
    }

    @Test
    public void translationsCoverGameplayMessages() {
        String json = resource("/assets/totembed/lang/en_us.json");
        assertTrue(json.contains("Totem Stasis"), "enchantment name must be translated");
        assertTrue(json.contains("message.totembed.bound"), "bind feedback must be translated");
        assertTrue(json.contains("message.totembed.returned"), "return feedback must be translated");
        assertTrue(json.contains("tooltip.totembed.stasis_linked"), "tooltip must be translated");
    }
}

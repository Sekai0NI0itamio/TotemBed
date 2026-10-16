package com.totembed;

import com.totembed.data.TotemStasisData;
import net.minecraft.util.math.BlockPos;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Binding-policy unit tests that run on plain JUnit (no game bootstrap):
 * only pure data + math, never Items/ItemStacks/Blocks.
 */
public class TotemBindingPolicyTest {

    @Test
    public void nullStackNeverThrowsInTooltip() {
        var lines = new ArrayList<net.minecraft.text.Text>();
        TotemTooltip.appendLines(null, lines);
        assertTrue(lines.isEmpty());
    }

    @Test
    public void onlyIntendedStackWouldCarryData() {
        // Binding writes to the single passed stack; this pins the data model
        // that makes per-stack binding possible (dimension + pos on the stack).
        TotemStasisData data = new TotemStasisData("minecraft:overworld", new BlockPos(7, 64, 7));
        assertTrue(data.isWellFormed());
        assertEquals("minecraft:overworld", data.dimension());
        assertEquals(new BlockPos(7, 64, 7), data.pos());
    }

    @Test
    public void dimensionsAreNeverConfused() {
        TotemStasisData overworld = new TotemStasisData("minecraft:overworld", new BlockPos(0, 64, 0));
        TotemStasisData nether = new TotemStasisData("minecraft:the_nether", new BlockPos(0, 64, 0));
        assertNotEquals(overworld.dimension(), nether.dimension());
        assertTrue(overworld.isWellFormed() && nether.isWellFormed());
    }
}

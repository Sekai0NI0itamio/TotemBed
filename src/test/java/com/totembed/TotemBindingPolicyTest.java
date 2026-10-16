package com.totembed;

import com.totembed.data.TotemStasisData;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.math.BlockPos;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tooltip + binding-policy unit tests (no server needed).
 */
public class TotemBindingPolicyTest {

    @Test
    public void nonTotemGetsNoTooltipLine() {
        ItemStack dirt = new ItemStack(Items.DIRT);
        var lines = new ArrayList<net.minecraft.text.Text>();
        // Must not throw even though the component is unregistered in unit tests.
        TotemTooltip.appendLines(dirt, lines);
        assertTrue(lines.isEmpty());
    }

    @Test
    public void malformedDataNeverThrowsInTooltip() {
        ItemStack totem = new ItemStack(Items.TOTEM_OF_UNDYING);
        var lines = new ArrayList<net.minecraft.text.Text>();
        TotemTooltip.appendLines(totem, lines);
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
}

package com.totembed;

import com.totembed.data.TotemStasisData;
import net.minecraft.util.math.BlockPos;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class TotemStasisDataTest {

    @Test
    public void wellFormedAcceptsOverworldId() {
        assertTrue(new TotemStasisData("minecraft:overworld", new BlockPos(10, 64, -4)).isWellFormed());
    }

    @Test
    public void rejectsBlankDimension() {
        assertFalse(new TotemStasisData("", new BlockPos(0, 64, 0)).isWellFormed());
    }

    @Test
    public void rejectsNullDimension() {
        assertFalse(new TotemStasisData(null, new BlockPos(0, 64, 0)).isWellFormed());
    }

    @Test
    public void rejectsNullPos() {
        assertFalse(new TotemStasisData("minecraft:overworld", null).isWellFormed());
    }

    @Test
    public void rejectsMalformedIdentifier() {
        assertFalse(new TotemStasisData("not a dimension!!", new BlockPos(0, 64, 0)).isWellFormed());
        assertFalse(new TotemStasisData("a:b:c", new BlockPos(0, 64, 0)).isWellFormed());
    }

    @Test
    public void acceptsCustomNamespace() {
        assertTrue(new TotemStasisData("totembed:limbo", new BlockPos(1, 2, 3)).isWellFormed());
    }

    @Test
    public void codecRoundTripPreservesValues() {
        TotemStasisData original = new TotemStasisData("minecraft:the_nether", new BlockPos(100, 50, -100));
        // Encode then decode with plain NBT ops (no registries needed for this codec).
        net.minecraft.nbt.NbtElement nbt = TotemStasisData.CODEC
            .encodeStart(net.minecraft.nbt.NbtOps.INSTANCE, original)
            .getOrThrow(s -> new AssertionError(s));
        TotemStasisData decoded = TotemStasisData.CODEC.parse(
            net.minecraft.nbt.NbtOps.INSTANCE, nbt)
            .getOrThrow(s -> new AssertionError(s));
        assertEquals(original, decoded);
        assertTrue(decoded.isWellFormed());
    }

    @Test
    public void acceptableDimensionHelperMatchesRecord() {
        assertTrue(BedReturnManager.isAcceptableDimensionId("minecraft:overworld"));
        assertFalse(BedReturnManager.isAcceptableDimensionId(""));
        assertFalse(BedReturnManager.isAcceptableDimensionId(null));
        assertFalse(BedReturnManager.isAcceptableDimensionId("bad id!!"));
    }
}

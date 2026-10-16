package com.totembed;

import net.minecraft.block.BedBlock;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.block.enums.BedPart;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pure-logic tests for bed-half resolution. These run without a Minecraft
 * server (plain JUnit) and verify the head/foot math used at bind time.
 */
public class BedHalfResolutionTest {

    @Test
    public void headClickResolvesToItself() {
        BlockPos clicked = new BlockPos(5, 64, 5);
        var state = Blocks.WHITE_BED.getDefaultState()
            .with(BedBlock.PART, BedPart.HEAD)
            .with(BedBlock.FACING, Direction.NORTH);
        assertEquals(clicked, BedBindingHandler.resolveCanonicalBedPos(state, clicked));
    }

    @Test
    public void footClickResolvesOneStepAlongFacing() {
        BlockPos foot = new BlockPos(5, 64, 5);
        for (Direction dir : new Direction[]{Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST}) {
            var state = Blocks.RED_BED.getDefaultState()
                .with(BedBlock.PART, BedPart.FOOT)
                .with(BedBlock.FACING, dir);
            assertEquals(foot.offset(dir), BedBindingHandler.resolveCanonicalBedPos(state, foot),
                "facing=" + dir);
        }
    }

    @Test
    public void nonBedReturnsNull() {
        BlockPos pos = new BlockPos(0, 64, 0);
        assertNull(BedBindingHandler.resolveCanonicalBedPos(
            Blocks.STONE.getDefaultState(), pos));
    }

    @Test
    public void headAndFootAreAdjacentPair() {
        // Sanity: head.offset(opposite) must be the foot for a NORTH bed.
        BlockPos head = new BlockPos(0, 64, 0);
        BlockPos foot = head.offset(Direction.NORTH.getOpposite());
        assertEquals(new BlockPos(0, 64, 1), foot);
    }
}

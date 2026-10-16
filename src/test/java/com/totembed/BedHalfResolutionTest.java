package com.totembed;

import net.minecraft.block.enums.BedPart;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pure-logic tests for bed-half resolution. No block lookups and no
 * registries: only enums + positions, so they run on plain JUnit without a
 * bootstrapped game.
 */
public class BedHalfResolutionTest {

    @Test
    public void headClickResolvesToItself() {
        BlockPos clicked = new BlockPos(5, 64, 5);
        for (Direction dir : Direction.values()) {
            if (!dir.getAxis().isHorizontal()) continue;
            assertEquals(clicked, BedBindingHandler.resolveHeadPos(BedPart.HEAD, dir, clicked));
        }
    }

    @Test
    public void footClickResolvesOneStepAlongFacing() {
        BlockPos foot = new BlockPos(5, 64, 5);
        for (Direction dir : new Direction[]{Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST}) {
            assertEquals(foot.offset(dir),
                BedBindingHandler.resolveHeadPos(BedPart.FOOT, dir, foot),
                "facing=" + dir);
        }
    }

    @Test
    public void nullInputsResolveToNull() {
        assertNull(BedBindingHandler.resolveHeadPos(null, Direction.NORTH, new BlockPos(0, 64, 0)));
        assertNull(BedBindingHandler.resolveHeadPos(BedPart.HEAD, null, new BlockPos(0, 64, 0)));
        assertNull(BedBindingHandler.resolveHeadPos(BedPart.HEAD, Direction.NORTH, null));
    }

    @Test
    public void headAndFootAreAdjacentPair() {
        // Sanity: head.offset(opposite) must be the foot for a NORTH bed.
        BlockPos head = new BlockPos(0, 64, 0);
        BlockPos foot = head.offset(Direction.NORTH.getOpposite());
        assertEquals(new BlockPos(0, 64, 1), foot);
    }
}

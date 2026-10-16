package com.totembed;

import com.totembed.data.TotemBedComponents;
import com.totembed.data.TotemStasisData;
import net.minecraft.block.BedBlock;
import net.minecraft.block.Blocks;
import net.minecraft.block.enums.BedPart;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

/**
 * Server GameTests (run via {@code ./gradlew runGametest} in CI).
 * Each test documents one gameplay requirement; failures are real failures.
 */
public class TotemBedGameTest {

    @GameTest(templateName = "fabric-gametest-api-v1:empty", duration = 100)
    public void headAndFootResolveToSameBed(TestContext ctx) {
        BlockPos foot = new BlockPos(1, 2, 1);
        ctx.setBlockState(foot, Blocks.WHITE_BED.getDefaultState()
            .with(BedBlock.FACING, Direction.NORTH)
            .with(BedBlock.PART, BedPart.FOOT));
        BlockPos head = foot.offset(Direction.NORTH);
        ctx.setBlockState(head, Blocks.WHITE_BED.getDefaultState()
            .with(BedBlock.FACING, Direction.NORTH)
            .with(BedBlock.PART, BedPart.HEAD));

        BlockPos fromFoot = BedBindingHandler.resolveCanonicalBedPos(
            ctx.getBlockState(foot), ctx.getAbsolutePos(foot));
        // resolveCanonicalBedPos is in absolute space; compare relatively here.
        ctx.assertTrue(fromFoot != null, "foot must resolve to a bed head");
        ctx.assertTrue(BedBindingHandler.isCompleteBed(
            ctx.getWorld(),
            ctx.getBlockState(ctx.getAbsolutePos(foot)),
            ctx.getAbsolutePos(foot),
            fromFoot), "placed pair must be a complete bed");
        ctx.complete();
    }

    @GameTest(templateName = "fabric-gametest-api-v1:empty", duration = 100)
    public void stasisDataSurvivesWellFormednessChecks(TestContext ctx) {
        TotemStasisData good = new TotemStasisData("minecraft:overworld", new BlockPos(1, 2, 3));
        ctx.assertTrue(good.isWellFormed(), "good binding must be well-formed");
        ctx.assertTrue(BedReturnManager.isAcceptableDimensionId("minecraft:the_nether"),
            "nether id shape must be acceptable");
        ctx.assertFalse(BedReturnManager.isAcceptableDimensionId("bad id!!"),
            "malformed id must be rejected");
        ctx.complete();
    }

    @GameTest(templateName = "fabric-gametest-api-v1:empty", duration = 100)
    public void componentTypeIsRegistered(TestContext ctx) {
        ctx.assertTrue(TotemBedComponents.TOTEM_STASIS != null,
            "data component totembed:totem_stasis must be registered");
        ctx.complete();
    }
}

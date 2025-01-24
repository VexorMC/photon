package dev.vexor.photon.mixin;

import dev.vexor.photon.Config;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.block.BlockModelRenderer;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.util.crash.CrashException;
import net.minecraft.util.crash.CrashReport;
import net.minecraft.util.crash.CrashReportSection;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockView;
import net.optifine.shaders.SVertexBuilder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockModelRenderer.class)
public abstract class MixinBlockModelRenderer {
    @Shadow public abstract boolean renderSmooth(BlockView world, BakedModel model, Block block, BlockPos pos, BufferBuilder buffer, boolean cull);

    @Shadow public abstract boolean renderFlat(BlockView world, BakedModel model, Block block, BlockPos pos, BufferBuilder buffer, boolean cull);

    /**
     * @reason Shaders
     * @author Lunasa
     */
    @Overwrite
    public boolean render(BlockView world, BakedModel model, BlockState state, BlockPos pos, BufferBuilder buffer, boolean cull) {
        boolean bl = MinecraftClient.isAmbientOcclusionEnabled() && state.getBlock().getLightLevel() == 0 && model.useAmbientOcclusion();

        try {
            if (Config.isShaders()) {
                SVertexBuilder.pushEntity(state, pos, world, buffer);
            }

            Block block = state.getBlock();
            boolean flag1 = bl ? this.renderSmooth(world, model, block, pos, buffer, cull) : this.renderFlat(world, model, block, pos, buffer, cull);

            if (Config.isShaders()) {
                SVertexBuilder.popEntity(buffer);
            }

            return flag1;
        } catch (Throwable throwable) {
            CrashReport crashReport = CrashReport.create(throwable, "Tesselating block model");
            CrashReportSection crashReportSection = crashReport.addElement("Block model being tesselated");
            CrashReportSection.addBlockInfo(crashReportSection, pos, state);
            crashReportSection.add("Using AO", bl);
            throw new CrashException(crashReport);
        }
    }
}
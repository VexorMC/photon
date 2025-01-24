package dev.vexor.photon.mixin;

import dev.vexor.photon.Config;
import net.minecraft.block.BlockState;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.block.FluidRenderer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockView;
import net.optifine.shaders.SVertexBuilder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(FluidRenderer.class)
public class MixinFluidRenderer {
    @Inject(method = "render", at = @At("HEAD"))
    void impl$render$begin(BlockView world, BlockState state, BlockPos pos, BufferBuilder buffer, CallbackInfoReturnable<Boolean> cir) {
        if (Config.isShaders()) {
            SVertexBuilder.pushEntity(state, pos, world, buffer);
        }
    }
    @Inject(method = "render", at = @At("RETURN"))
    void impl$render$end(BlockView world, BlockState state, BlockPos pos, BufferBuilder buffer, CallbackInfoReturnable<Boolean> cir) {
        if (Config.isShaders()) {
            SVertexBuilder.popEntity(buffer);
        }
    }
}

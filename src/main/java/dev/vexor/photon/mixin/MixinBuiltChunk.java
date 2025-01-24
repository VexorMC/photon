package dev.vexor.photon.mixin;

import dev.vexor.photon.Config;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.world.BuiltChunk;
import net.minecraft.client.world.ChunkAssemblyHelper;
import net.optifine.shaders.SVertexBuilder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(BuiltChunk.class)
public abstract class MixinBuiltChunk {
    @Shadow protected abstract void method_10157(RenderLayer renderLayer, float f, float g, float h, BufferBuilder bufferBuilder, ChunkAssemblyHelper chunkAssemblyHelper);

    @Redirect(method = "method_10164", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/world/BuiltChunk;method_10157(Lnet/minecraft/client/render/RenderLayer;FFFLnet/minecraft/client/render/BufferBuilder;Lnet/minecraft/client/world/ChunkAssemblyHelper;)V"))
    void impl$postRenderBlocks(BuiltChunk instance, RenderLayer renderLayer, float f, float g, float h, BufferBuilder bufferBuilder, ChunkAssemblyHelper chunkAssemblyHelper) {
        if (Config.isShaders()) {
            SVertexBuilder.calcNormalChunkLayer(bufferBuilder);
        }
        this.method_10157(renderLayer, f, g, h, bufferBuilder, chunkAssemblyHelper);
    }
}

package dev.vexor.photon.mixin;

import dev.vexor.photon.Config;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.util.TexturedQuad;
import net.optifine.shaders.SVertexFormat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(TexturedQuad.class)
public class MixinTexturedQuad {
    @Redirect(method = "draw", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/BufferBuilder;begin(ILnet/minecraft/client/render/VertexFormat;)V"))
    void impl$draw(BufferBuilder instance, int drawMode, VertexFormat format) {
        if (Config.isShaders()) {
            instance.begin(drawMode, SVertexFormat.defVertexFormatTextured);
        } else {
            instance.begin(drawMode, format);
        }
    }
}

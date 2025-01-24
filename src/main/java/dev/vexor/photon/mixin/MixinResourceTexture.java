package dev.vexor.photon.mixin;

import dev.vexor.photon.Config;
import dev.vexor.photon.tex.ExtendedTexture;
import net.minecraft.client.texture.AbstractTexture;
import net.minecraft.client.texture.ResourceTexture;
import net.minecraft.client.texture.TextureUtil;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;
import net.optifine.shaders.ShadersTex;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.awt.image.BufferedImage;

@Mixin(ResourceTexture.class)
public abstract class MixinResourceTexture extends AbstractTexture {
    @Shadow @Final protected Identifier field_6555;

    @Redirect(method = "load", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/texture/TextureUtil;method_5860(ILjava/awt/image/BufferedImage;ZZ)I"))
    int impl$load(int id, BufferedImage image, boolean linear, boolean clamp, ResourceManager resourceManager) {
        if (Config.isShaders()) {
            return ShadersTex.loadSimpleTexture(this.getGlId(), image, linear, clamp, resourceManager, this.field_6555, ((ExtendedTexture) this).getMultiTex());
        } else {
            return TextureUtil.method_5860(this.getGlId(), image, linear, clamp);
        }
    }
}

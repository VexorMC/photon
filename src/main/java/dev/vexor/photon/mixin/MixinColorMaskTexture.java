package dev.vexor.photon.mixin;

import dev.vexor.photon.Config;
import dev.vexor.photon.tex.ExtendedTexture;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.ColorMaskTexture;
import net.minecraft.client.texture.TextureUtil;
import net.minecraft.util.Identifier;
import net.optifine.shaders.ShadersTex;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.awt.image.BufferedImage;

@Mixin(ColorMaskTexture.class)
public class MixinColorMaskTexture {
    @Shadow @Final private Identifier identifier;

    @Redirect(method = "load", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/texture/TextureUtil;method_5858(ILjava/awt/image/BufferedImage;)I"))
    int impl$method_5858(int i, BufferedImage bufferedImage) {
        if (Config.isShaders()) {
            ShadersTex.loadSimpleTexture(i, bufferedImage, false, false, MinecraftClient.getInstance().getResourceManager(), this.identifier, ((ExtendedTexture) this).getMultiTex());
        } else {
            TextureUtil.method_5858(i, bufferedImage);
        }
        return i;
    }
}

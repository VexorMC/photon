package dev.vexor.photon.mixin;

import com.mojang.blaze3d.platform.GlStateManager;
import dev.vexor.photon.Config;
import net.minecraft.client.texture.Texture;
import net.minecraft.client.texture.TextureManager;
import net.minecraft.client.texture.TextureUtil;
import net.minecraft.util.Identifier;
import net.optifine.shaders.ShadersTex;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Map;

@Mixin(TextureManager.class)
public class MixinTextureManager {
    @Shadow @Final private Map<Identifier, Texture> textures;

    @Redirect(method = "bindTexture", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/texture/TextureUtil;bindTexture(I)V"))
    void impl$bindTexture(int texture, Identifier id) {
        Texture textureObject = this.textures.get(id);
        if (Config.isShaders()) {
            ShadersTex.bindTexture(textureObject);
        } else {
            GlStateManager.bindTexture(textureObject.getGlId());
        }
    }
}

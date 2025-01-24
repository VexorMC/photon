package dev.vexor.photon.mixin;

import dev.vexor.photon.Config;
import dev.vexor.photon.tex.ExtendedTexture;
import net.minecraft.client.texture.PlayerSkinTexture;
import net.minecraft.client.texture.ResourceTexture;
import net.minecraft.client.texture.TextureUtil;
import net.minecraft.util.Identifier;
import net.optifine.shaders.ShadersTex;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.awt.image.BufferedImage;

@Mixin(PlayerSkinTexture.class)
public abstract class MixinPlayerSkinTexture extends ResourceTexture {
    @Shadow private boolean field_6553;

    @Shadow private BufferedImage field_6550;

    public MixinPlayerSkinTexture(Identifier identifier) {
        super(identifier);
    }

    /**
     * @reason Texture support for shaders
     * @author Lunasa
     */
    @Overwrite
    void method_6997() {
        if (!this.field_6553 && this.field_6550 != null) {
            this.field_6553 = true;

            if (this.field_6555 != null) this.clearGlId();

            if (Config.isShaders()) ShadersTex.loadSimpleTexture(super.getGlId(), this.field_6550, false, false, Config.getResourceManager(), this.field_6555, ((ExtendedTexture)this).getMultiTex());
            else TextureUtil.method_5858(super.getGlId(), this.field_6550);
        }
    }
}

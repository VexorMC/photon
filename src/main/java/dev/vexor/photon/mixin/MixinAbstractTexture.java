package dev.vexor.photon.mixin;

import dev.vexor.photon.tex.ExtendedTexture;
import net.minecraft.client.texture.AbstractTexture;
import net.optifine.shaders.MultiTexID;
import net.optifine.shaders.ShadersTex;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractTexture.class)
public class MixinAbstractTexture implements ExtendedTexture {
    @Shadow protected int glId;
    @Unique
    private MultiTexID id;

    @Inject(method = "clearGlId", at = @At("RETURN"))
    void impl$clearGlId(CallbackInfo ci) {
        ShadersTex.deleteTextures((AbstractTexture)(Object)this, this.glId);
    }

    @Override
    public MultiTexID getMultiTex() {
        return ShadersTex.getMultiTexID((AbstractTexture)(Object)this);
    }

    @Override
    public MultiTexID getMultiTexDirect() {
        return id;
    }

    @Override
    public void setMultiTex(MultiTexID id) {
        this.id = id;
    }
}

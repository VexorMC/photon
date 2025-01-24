package dev.vexor.photon.mixin;

import net.minecraft.client.render.model.ModelPart;
import net.optifine.shaders.Shaders;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ModelPart.class)
public class MixinModelPart {
    @Shadow private boolean compiledList;
    @Unique private int countResetDisplayList;

    @Inject(method = "render", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/platform/GlStateManager;translate(FFF)V", ordinal = 0))
    void impl$render(float scale, CallbackInfo ci) {
        this.checkResetDisplayList();
    }

    @Inject(method = "rotateAndRender", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/platform/GlStateManager;translate(FFF)V", ordinal = 0))
    void impl$rotateAndRender(float scale, CallbackInfo ci) {
        this.checkResetDisplayList();
    }

    @Inject(method = "preRender", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/platform/GlStateManager;translate(FFF)V", ordinal = 0))
    void impl$preRender(float scale, CallbackInfo ci) {
        this.checkResetDisplayList();
    }

    @Unique
    private void checkResetDisplayList() {
        if (this.countResetDisplayList != Shaders.countResetDisplayLists) {
            this.compiledList = false;
            this.countResetDisplayList = Shaders.countResetDisplayLists;
        }
    }
}
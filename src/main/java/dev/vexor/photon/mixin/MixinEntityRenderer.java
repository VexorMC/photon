package dev.vexor.photon.mixin;

import dev.vexor.photon.Config;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.entity.Entity;
import net.optifine.shaders.Shaders;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityRenderer.class)
public class MixinEntityRenderer {
    @Inject(method = "renderShadow", at = @At("HEAD"), cancellable = true)
    void impl$renderShadow(Entity entity, double x, double y, double z, float f, float tickDelta, CallbackInfo ci) {
        if (Config.isShaders() || Shaders.shouldSkipDefaultShadow) ci.cancel();
    }
}

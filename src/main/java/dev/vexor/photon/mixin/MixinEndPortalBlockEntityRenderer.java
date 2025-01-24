package dev.vexor.photon.mixin;

import dev.vexor.photon.Config;
import net.minecraft.block.entity.EndPortalBlockEntity;
import net.minecraft.client.render.block.entity.EndPortalBlockEntityRenderer;
import net.optifine.shaders.ShadersRender;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EndPortalBlockEntityRenderer.class)
public class MixinEndPortalBlockEntityRenderer {
    @Inject(method = "render(Lnet/minecraft/block/entity/EndPortalBlockEntity;DDDFI)V", at = @At("HEAD"), cancellable = true)
    void impl$render(EndPortalBlockEntity endPortalBlockEntity, double x, double y, double z, float tickDelta, int stage, CallbackInfo ci) {
        if (Config.isShaders() || ShadersRender.renderEndPortal(endPortalBlockEntity, x, y, z, tickDelta, stage, 0.75F)) ci.cancel();
    }
}

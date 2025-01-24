package dev.vexor.photon.mixin;

import dev.vexor.photon.Config;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.entity.ItemFrameEntityRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.ItemFrameEntity;
import net.optifine.shaders.Shaders;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemFrameEntityRenderer.class)
public class MixinItemFrameEntityRenderer {
    @Inject(method = "method_4334", at = @At("HEAD"), cancellable = true)
    public void impl$method_4334(ItemFrameEntity itemFrameEntity, CallbackInfo ci) {
        if (Shaders.isShadowPass) {
            ci.cancel();
        } else {
            if (!Config.zoomMode) {
                Entity entity = MinecraftClient.getInstance().getCameraEntity();
                double d0 = itemFrameEntity.distanceTo(entity.x, entity.y, entity.z);

                if (d0 > 4096.0D) {
                    ci.cancel();
                }
            }
        }
    }
}

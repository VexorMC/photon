package dev.vexor.photon.mixin;

import dev.vexor.photon.Config;
import net.minecraft.block.entity.BeaconBlockEntity;
import net.minecraft.client.render.block.entity.BeaconBlockEntityRenderer;
import net.optifine.shaders.Shaders;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BeaconBlockEntityRenderer.class)
public class MixinBeaconBlockEntityRenderer {
    @Inject(method = "render(Lnet/minecraft/block/entity/BeaconBlockEntity;DDDFI)V", at = @At("HEAD"))
    void impl$render$begin(BeaconBlockEntity beaconBlockEntity, double x, double y, double z, float tickDelta, int light, CallbackInfo ci) {
        float speed = beaconBlockEntity.getBeamSpeed();

        if (speed > 0.0D && Config.isShaders()) {
            Shaders.beginBeacon();
        }
    }

    @Inject(method = "render(Lnet/minecraft/block/entity/BeaconBlockEntity;DDDFI)V", at = @At("RETURN"))
    void impl$render$end(BeaconBlockEntity beaconBlockEntity, double x, double y, double z, float tickDelta, int light, CallbackInfo ci) {
        float speed = beaconBlockEntity.getBeamSpeed();

        if (speed > 0.0D && Config.isShaders()) {
            Shaders.endBeacon();
        }
    }
}

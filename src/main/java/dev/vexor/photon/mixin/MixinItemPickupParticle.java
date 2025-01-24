package dev.vexor.photon.mixin;

import dev.vexor.photon.Config;
import net.minecraft.client.particle.ItemPickupParticle;
import net.minecraft.entity.Entity;
import net.optifine.shaders.Program;
import net.optifine.shaders.Shaders;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemPickupParticle.class)
public class MixinItemPickupParticle {
    @Shadow private Entity itemEntity;

    @Unique Program program = null;

    @Inject(method = "draw", at = @At("HEAD"))
    void impl$draw$begin(CallbackInfo ci) {
        if (Config.isShaders()) {
            program = Shaders.activeProgram;
            Shaders.nextEntity(this.itemEntity);
        }
    }

    @Inject(method = "draw", at = @At("HEAD"))
    void impl$draw$end(CallbackInfo ci) {
        if (Config.isShaders()) {
            Shaders.setEntityId(null);
            Shaders.useProgram(program);
        }
    }
}

package dev.vexor.photon.mixin;

import dev.vexor.photon.Config;
import net.minecraft.client.render.entity.feature.DragonEyesFeatureRenderer;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.entity.Entity;
import net.optifine.shaders.Shaders;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(DragonEyesFeatureRenderer.class)
public class MixinDragonEyesFeatureRenderer {
    @Redirect(method = "render(Lnet/minecraft/entity/boss/dragon/EnderDragonEntity;FFFFFFF)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/entity/model/EntityModel;render(Lnet/minecraft/entity/Entity;FFFFFF)V"))
    void impl$render$shaders(EntityModel instance, Entity entity, float handSwing, float handSwingAmount, float tickDelta, float age, float headPitch, float scale) {
        if (Config.isShaders()) {
            Shaders.beginSpiderEyes();
        }

        instance.render(entity, handSwing, handSwingAmount, tickDelta, age, headPitch, scale);

        if (Config.isShaders()) {
            Shaders.endSpiderEyes();
        }
    }
}

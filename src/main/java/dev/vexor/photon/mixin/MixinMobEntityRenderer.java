package dev.vexor.photon.mixin;

import dev.vexor.photon.Config;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.MobEntity;
import net.optifine.shaders.Shaders;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MobEntityRenderer.class)
public abstract class MixinMobEntityRenderer<T extends MobEntity> extends LivingEntityRenderer<T> {
    public MixinMobEntityRenderer(EntityRenderDispatcher entityRenderDispatcher, EntityModel entityModel, float f) {
        super(entityRenderDispatcher, entityModel, f);
    }

    @Inject(method = "method_5792", at = @At("HEAD"), cancellable = true)
    void impl$method_5792(T mobEntity, double d, double e, double f, float g, float h, CallbackInfo ci) {
        if (Config.isShaders() || Shaders.isShadowPass) ci.cancel();
    }

    @Inject(method = "method_5792", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/platform/GlStateManager;disableCull()V"))
    void impl$method_5792$begin(T mobEntity, double d, double e, double f, float g, float h, CallbackInfo ci) {
        if (Config.isShaders()) {
            Shaders.beginLeash();
        }
    }

    @Inject(method = "method_5792", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/Tessellator;draw()V", ordinal = 1))
    void impl$method_5792$end(T mobEntity, double d, double e, double f, float g, float h, CallbackInfo ci) {
        if (Config.isShaders()) {
            Shaders.endLeash();
        }
    }
}

package dev.vexor.photon.mixin;

import dev.vexor.photon.Config;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.optifine.shaders.Shaders;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntityRenderer.class)
public abstract class MixinLivingEntityRenderer<T extends LivingEntity> extends EntityRenderer<T> {
    @Shadow protected abstract int method_5776(T livingEntity, float f, float g);

    protected MixinLivingEntityRenderer(EntityRenderDispatcher entityRenderDispatcher) {
        super(entityRenderDispatcher);
    }

    @Inject(method = "method_10252", at = @At(value = "INVOKE", target = "Ljava/nio/FloatBuffer;put(F)Ljava/nio/FloatBuffer;", ordinal = 3))
    void impl$method_10252(T livingEntity, float f, boolean bl, CallbackInfoReturnable<Boolean> cir) {
        if (Config.isShaders()) {
            Shaders.setEntityColor(1.0F, 0.0F, 0.0F, 0.3F);
        }
    }
    @Inject(method = "method_10260", at = @At(value = "RETURN"))
    void impl$method_10252(CallbackInfo ci) {
        if (Config.isShaders()) {
            Shaders.setEntityColor(0.0F, 0.0F, 0.0F, 0.0F);
        }
    }
    @Inject(method = "method_10252", at = @At(value = "INVOKE", target = "Ljava/nio/FloatBuffer;put(F)Ljava/nio/FloatBuffer;", ordinal = 7))
    void impl$method_10252$2(T livingEntity, float f, boolean bl, CallbackInfoReturnable<Boolean> cir) {
        float g = livingEntity.getBrightnessAtEyes(f);
        int i = this.method_5776(livingEntity, g, f);
        float h = (float) (i >> 24 & 255) / 255.0F;
        float j = (float) (i >> 16 & 255) / 255.0F;
        float k = (float) (i >> 8 & 255) / 255.0F;
        float l = (float) (i & 255) / 255.0F;

        if (Config.isShaders()) {
            Shaders.setEntityColor(h, j, k, 1.0F - l);
        }
    }
}

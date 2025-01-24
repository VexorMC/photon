package dev.vexor.photon.mixin;

import com.mojang.blaze3d.platform.GlStateManager;
import dev.vexor.photon.Config;
import dev.vexor.photon.tex.ExtendedGameRenderer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.*;
import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.MathHelper;
import net.optifine.shaders.Shaders;
import net.optifine.shaders.ShadersRender;
import org.lwjgl.util.glu.Project;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.nio.FloatBuffer;

@Mixin(GameRenderer.class)
public abstract class MixinGameRenderer implements ExtendedGameRenderer {
    @Shadow @Final public HeldItemRenderer firstPersonRenderer;

    @Shadow private boolean renderingPanorama;

    @Shadow private MinecraftClient client;

    @Shadow private float viewDistance;

    @Shadow protected abstract float getFov(float tickDelta, boolean changingFov);

    @Shadow protected abstract void bobViewWhenHurt(float tickDelta);

    @Shadow protected abstract void bobView(float tickDelta);

    @Shadow public abstract void disableLightmap();

    @Shadow public abstract void enableLightmap();

    @Shadow public int frameCount;

    @Shadow protected abstract void renderWeather(float tickDelta);

    @Shadow private boolean renderHand;

    @Shadow protected abstract void renderDebugCrosshair(float tickDelta);

    @Shadow protected abstract void renderFog(int i, float tickDelta);

    @Inject(method = "disableLightmap", at = @At("RETURN"))
    void impl$disableLightmap(CallbackInfo ci) {
        if (Config.isShaders()) Shaders.disableLightmap();
    }

    @Inject(method = "enableLightmap", at = @At("RETURN"))
    void impl$enableLightmap(CallbackInfo ci) {
        if (Config.isShaders()) Shaders.enableLightmap();
    }

    @Inject(method = "renderWorld(FJ)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/GameRenderer;updateTargetedEntity(F)V"))
    void impl$renderWorld(float tickDelta, long limitTime, CallbackInfo ci) {
        if (Config.isShaders()) Shaders.beginRender(this.client, tickDelta, limitTime);
    }

    @Inject(method = "renderWorld(IFJ)V", at = @At(value = "HEAD"))
    void impl$renderWorldPass(int anaglyphFilter, float tickDelta, long limitTime, CallbackInfo ci) {
        if (Config.isShaders()) Shaders.beginRenderPass(anaglyphFilter, tickDelta, limitTime);
    }

    @Inject(method = "renderWorld(IFJ)V", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/platform/GlStateManager;clear(I)V", ordinal = 0))
    void impl$renderWorldPass$clear(int anaglyphFilter, float tickDelta, long limitTime, CallbackInfo ci) {
        if (Config.isShaders()) Shaders.clearRenderBuffer();
    }

    @Inject(method = "renderWorld(IFJ)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/GameRenderer;setupCamera(FI)V"))
    void impl$renderWorldPass$setupCamera(int anaglyphFilter, float tickDelta, long limitTime, CallbackInfo ci) {
        if (Config.isShaders()) Shaders.setCamera(tickDelta);
    }

    @Redirect(method = "renderWorld(IFJ)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/CameraView;setPos(DDD)V"))
    void impl$renderWorldPass$disableCulling(CameraView instance, double x, double y, double z) {
        if (Config.isShaders()) ShadersRender.setFrustrumPosition(instance, x, y, z);
        else instance.setPos(x, y, z);
    }

    @Redirect(method = "renderWorld(IFJ)V", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/platform/GlStateManager;viewport(IIII)V"))
    void impl$renderWorldPass$viewport(int x, int y, int width, int height) {
        if (Config.isShaders()) Shaders.setViewport(0, 0, width, height);
        else GlStateManager.viewport(0, 0, width, height);
    }

    @Redirect(method = "renderWorld(IFJ)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/WorldRenderer;setupTerrain(Lnet/minecraft/entity/Entity;DLnet/minecraft/client/render/CameraView;IZ)V"))
    void impl$renderWorldPass$setupTerrain(WorldRenderer instance, Entity entity, double tickDelta, CameraView cameraView, int frame, boolean spectator) {
        if (Config.isShaders()) ShadersRender.setupTerrain(instance, entity, tickDelta, cameraView, frame, this.client.player.isSpectator());
        else instance.setupTerrain(entity, (double)tickDelta, cameraView, frame, this.client.player.isSpectator());
    }

    @Inject(method = "renderWorld(IFJ)V", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/platform/GlStateManager;disableAlphaTest()V", ordinal = 0))
    void impl$renderWorldPass$layerHook$solid(int anaglyphFilter, float tickDelta, long limitTime, CallbackInfo ci) {
        if (Config.isShaders()) ShadersRender.beginTerrainSolid();
    }

    @Inject(method = "renderWorld(IFJ)V", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/platform/GlStateManager;enableAlphaTest()V", ordinal = 0))
    void impl$renderWorldPass$layerHook$cutoutMipped(int anaglyphFilter, float tickDelta, long limitTime, CallbackInfo ci) {
        if (Config.isShaders()) ShadersRender.beginTerrainCutoutMipped();
    }

    @Inject(method = "renderWorld(IFJ)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/texture/Texture;pushFilter(ZZ)V", ordinal = 0))
    void impl$renderWorldPass$layerHook$cutout(int anaglyphFilter, float tickDelta, long limitTime, CallbackInfo ci) {
        if (Config.isShaders()) ShadersRender.beginTerrainCutout();
    }

    @Inject(method = "renderWorld(IFJ)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/texture/Texture;pop()V", ordinal = 0))
    void impl$renderWorldPass$layerHook$end(int anaglyphFilter, float tickDelta, long limitTime, CallbackInfo ci) {
        if (Config.isShaders()) ShadersRender.endTerrain();
    }

    @Inject(method = "renderWorld(IFJ)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/profiler/Profiler;swap(Ljava/lang/String;)V", ordinal = 13))
    void impl$renderWorldPass$particles$lit$begin(int anaglyphFilter, float tickDelta, long limitTime, CallbackInfo ci) {
        if (Config.isShaders()) Shaders.beginLitParticles();
    }

    /**
     * @reason Shaders
     * @author Lunasa
     */
    @Overwrite
    private void renderClouds(WorldRenderer worldRenderer, float tickDelta, int anaglyphFilter) {
        if (this.client.options.viewDistance >= 4 && Shaders.shouldRenderClouds(this.client.options)) {
            this.client.profiler.swap("clouds");
            GlStateManager.matrixMode(5889);
            GlStateManager.loadIdentity();
            Project.gluPerspective(this.getFov(tickDelta, true), (float)this.client.width / (float)this.client.height, 0.05F, this.viewDistance * 4.0F);
            GlStateManager.matrixMode(5888);
            GlStateManager.pushMatrix();
            this.renderFog(0, tickDelta);
            worldRenderer.renderClouds(tickDelta, anaglyphFilter);
            GlStateManager.disableFog();
            GlStateManager.popMatrix();
            GlStateManager.matrixMode(5889);
            GlStateManager.loadIdentity();
            Project.gluPerspective(this.getFov(tickDelta, true), (float)this.client.width / (float)this.client.height, 0.05F, this.viewDistance * MathHelper.SQUARE_ROOT_OF_TWO);
            GlStateManager.matrixMode(5888);
        }
    }

    @Inject(method = "renderWorld(IFJ)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/profiler/Profiler;swap(Ljava/lang/String;)V", ordinal = 14))
    void impl$renderWorldPass$particles$begin(int anaglyphFilter, float tickDelta, long limitTime, CallbackInfo ci) {
        if (Config.isShaders()) Shaders.beginParticles();
    }

    @Inject(method = "renderWorld(IFJ)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/particle/ParticleManager;renderParticles(Lnet/minecraft/entity/Entity;F)V", shift = At.Shift.AFTER))
    void impl$renderWorldPass$particles$end(int anaglyphFilter, float tickDelta, long limitTime, CallbackInfo ci) {
        if (Config.isShaders()) Shaders.endParticles();
    }

    @Inject(method = "renderWorld(IFJ)V", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/platform/GlStateManager;depthMask(Z)V", ordinal = 0, shift = At.Shift.AFTER))
    void impl$renderWorldPass$rainDepth(int anaglyphFilter, float tickDelta, long limitTime, CallbackInfo ci) {
        if (Config.isShaders()) GlStateManager.depthMask(Shaders.isRainDepth());
    }

    @Redirect(method = "renderWorld(IFJ)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/GameRenderer;renderWeather(F)V"))
    void impl$renderWorldPass$weather(GameRenderer instance, float tickDelta) {
        if (Config.isShaders()) Shaders.beginWeather();
        this.renderWeather(tickDelta);
        if (Config.isShaders()) Shaders.endWeather();
    }

    @Redirect(method = "renderWorld(IFJ)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/WorldRenderer;renderSky(FI)V"))
    void impl$renderWorldPass$sky(WorldRenderer instance, float tickDelta, int anaglyphFilter) {
        if (Config.isShaders()) Shaders.beginSky();
        instance.renderSky(tickDelta, anaglyphFilter);
        if (Config.isShaders()) Shaders.endSky();
    }

    @Inject(method = "renderWorld(IFJ)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/WorldRenderer;renderWorldBorder(Lnet/minecraft/entity/Entity;F)V", shift = At.Shift.AFTER))
    void impl$renderWorldPass$water$pre(int anaglyphFilter, float tickDelta, long limitTime, CallbackInfo ci) {
        if (Config.isShaders()) {
            ShadersRender.renderHand0((GameRenderer) (Object) this, tickDelta, anaglyphFilter);
            Shaders.preWater();
        }
    }

    @Inject(method = "renderWorld(IFJ)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/WorldRenderer;renderWorldBorder(Lnet/minecraft/entity/Entity;F)V", shift = At.Shift.AFTER))
    void impl$renderWorldPass$hand$final(int anaglyphFilter, float tickDelta, long limitTime, CallbackInfo ci) {
        if (Config.isShaders()) {
            ShadersRender.renderHand0((GameRenderer) (Object) this, tickDelta, anaglyphFilter);
            Shaders.preWater();
        }
    }


    @Inject(method = "updateFogColorBuffer", at = @At(value = "RETURN"))
    void impl$updateFogColorBuffer(float red, float green, float blue, float alpha, CallbackInfoReturnable<FloatBuffer> cir) {
        if (Config.isShaders()) Shaders.setFogColor(red, green, blue);
    }


    @Redirect(method = "renderWorld(IFJ)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/WorldRenderer;renderLayer(Lnet/minecraft/client/render/RenderLayer;DILnet/minecraft/entity/Entity;)I", ordinal = 3))
    int impl$renderWorldPass$transclucent(WorldRenderer instance, RenderLayer renderLayer, double tickDelta, int anaglyphFilter, Entity entity) {
        if (Config.isShaders()) ShadersRender.beginTranslucent();
        int i = instance.renderLayer(renderLayer, tickDelta, anaglyphFilter, entity);
        if (Config.isShaders()) ShadersRender.endTranslucent();
        return i;
    }

    @Inject(
            method = "renderWorld(IFJ)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/util/profiler/Profiler;swap(Ljava/lang/String;)V", ordinal = 18),
            cancellable = true
    )
    private void onRenderWorld(int pass, float delta, long limit, CallbackInfo ci) {
        if (this.renderHand && !Shaders.isShadowPass) {
            if (Config.isShaders()) {
                ShadersRender.renderHand1((GameRenderer) (Object) this, delta, pass);
                Shaders.renderCompositeFinal();
            }
            GlStateManager.clear(256);

            if (Config.isShaders()) ShadersRender.renderFPOverlay((GameRenderer) (Object) this, delta, pass);
            else this.renderHand(delta, pass);

            this.renderDebugCrosshair(delta);

            if (Config.isShaders()) {
                Shaders.endRender();
            }

            ci.cancel();
        }
    }

    @Redirect(method = "updateFog", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/platform/GlStateManager;clearColor(FFFF)V"))
    void impl$updateFog(float red, float green, float blue, float alpha) {
        Shaders.setClearColor(red, green, blue, 0.0F);
    }

    /**
     * @reason Shaders
     * @author Lunasa
     */
    @Overwrite
    public final void renderHand(float tickDelta, int anaglyphOffset) {
        renderHand(tickDelta, anaglyphOffset, true, true, false);
    }

    @Override
    public void renderHand(float tickDelta, int anaglyphOffset, boolean a, boolean b, boolean c) {
        if (!this.renderingPanorama) {
            GlStateManager.matrixMode(5889);
            GlStateManager.loadIdentity();
            final float f = 0.07F;
            if (this.client.options.anaglyph3d) {
                GlStateManager.translate((float)(-(anaglyphOffset * 2 - 1)) * f, 0.0F, 0.0F);
            }
            if (Config.isShaders()) Shaders.applyHandDepth();
            Project.gluPerspective(this.getFov(tickDelta, false), (float)this.client.width / (float)this.client.height, 0.05F, this.viewDistance * 2.0F);
            GlStateManager.matrixMode(5888);
            GlStateManager.loadIdentity();
            if (this.client.options.anaglyph3d) {
                GlStateManager.translate((float)(anaglyphOffset * 2 - 1) * 0.1F, 0.0F, 0.0F);
            }
            boolean flag = false;
            if (a) {
                GlStateManager.pushMatrix();
                this.bobViewWhenHurt(tickDelta);
                if (this.client.options.bobView) this.bobView(tickDelta);
                flag = this.client.getCameraEntity() instanceof LivingEntity && ((LivingEntity) this.client.getCameraEntity()).isSleeping();
                if (this.client.options.perspective == 0 && !flag && !this.client.options.hudHidden && !this.client.player.isSpectator()) {
                    this.enableLightmap();
                    if (Config.isShaders()) ShadersRender.renderItemFP(this.firstPersonRenderer, tickDelta, c);
                    else this.firstPersonRenderer.renderArmHoldingItem(tickDelta);
                    this.disableLightmap();
                }
                GlStateManager.popMatrix();
            }
            if (!b) return;
            this.disableLightmap();
            if (this.client.options.perspective == 0 && !flag) {
                this.firstPersonRenderer.renderOverlays(tickDelta);
                this.bobViewWhenHurt(tickDelta);
            }
            if (this.client.options.bobView) this.bobView(tickDelta);
        }
    }
}

package dev.vexor.photon.mixin;

import com.mojang.blaze3d.platform.GlStateManager;
import dev.vexor.photon.Config;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.ShaderEffect;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.render.BuiltChunkStorage;
import net.minecraft.client.render.CameraView;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.render.block.entity.BlockEntityRenderDispatcher;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.optifine.shaders.Shaders;
import net.optifine.shaders.ShadersRender;
import net.optifine.shaders.gui.GuiShaderOptions;
import org.lwjgl.input.Keyboard;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;

@Mixin(WorldRenderer.class)
public class MixinWorldRenderer {
    @Shadow private Framebuffer entityOutlineFramebuffer;

    @Shadow private ShaderEffect entityOutlineShader;

    @Shadow @Final private MinecraftClient client;

    @Shadow private ClientWorld world;

    @Unique boolean shadersEnabled;

    /**
     * @reason Shaders
     * @author Lunasa
     */
    @Overwrite
    public boolean isEntityOutline() {
        return !Config.isShaders() && this.entityOutlineFramebuffer != null && this.entityOutlineShader != null && this.client.player != null && this.client.player.isSpectator() && this.client.options.spectatorOutlines.isPressed();
    }

    @Inject(method = "setWorld", at = @At(value = "FIELD", target = "Lnet/minecraft/client/render/WorldRenderer;world:Lnet/minecraft/client/world/ClientWorld;", ordinal = 2))
    void impl$setWorld(CallbackInfo ci) {
        Shaders.checkWorldChanged(this.world);
    }

    @Inject(method = "renderEntities", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/profiler/Profiler;swap(Ljava/lang/String;)V", ordinal = 2))
    void impl$renderEntities$begin(CallbackInfo ci) {
        shadersEnabled = Config.isShaders();
        if (shadersEnabled) Shaders.beginEntities();
    }

    @Redirect(method = "renderEntities", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/entity/EntityRenderDispatcher;renderEntity(Lnet/minecraft/entity/Entity;F)Z", ordinal = 2))
    boolean impl$renderEntities$redirect$renderEntity(EntityRenderDispatcher instance, Entity entity, float f) {
        if (shadersEnabled) Shaders.nextEntity(entity);
        return instance.renderEntity(entity, f);
    }

    @Redirect(method = "renderEntities", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/entity/EntityRenderDispatcher;method_10204(Lnet/minecraft/entity/Entity;F)V"))
    void impl$renderEntities$redirect$method_10204(EntityRenderDispatcher instance, Entity entity, float f) {
        if (shadersEnabled) Shaders.nextEntity(entity);
        instance.renderEntity(entity, f);
    }

    @Inject(method = "renderEntities", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/profiler/Profiler;swap(Ljava/lang/String;)V", ordinal = 3))
    void impl$renderEntities$switchBE(CallbackInfo ci) {
        if (shadersEnabled) {
            Shaders.endEntities();
            Shaders.beginBlockEntities();
        }
    }

    @Redirect(method = "renderEntities", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/block/entity/BlockEntityRenderDispatcher;renderEntity(Lnet/minecraft/block/entity/BlockEntity;FI)V"))
    void impl$renderEntities$renderBE(BlockEntityRenderDispatcher instance, BlockEntity blockEntity, float tickDelta, int destroyProgress) {
        if (shadersEnabled) {
            Shaders.nextBlockEntity(blockEntity);
        }

        instance.renderEntity(blockEntity, tickDelta, destroyProgress);
    }

    @Inject(method = "renderEntities", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/WorldRenderer;postDrawBlockDamage()V"))
    void impl$renderEntities$postDrawBlockDamage(CallbackInfo ci) {
        if (shadersEnabled) Shaders.endBlockEntities();
    }

    @Inject(method = "tick", at = @At("HEAD"))
    void impl$tick(CallbackInfo ci) {
        if (Config.isShaders()) {
            if (Keyboard.isKeyDown(61) && Keyboard.isKeyDown(24)) {
                final GuiShaderOptions guishaderoptions = new GuiShaderOptions(null, Config.getGameSettings());
                Config.getMinecraft().setScreen(guishaderoptions);
            }
            if (Keyboard.isKeyDown(61) && Keyboard.isKeyDown(19)) {
                Shaders.uninit();
                Shaders.loadShaderPack();
            }
        }
    }

    @Inject(method = "renderLayer(Lnet/minecraft/client/render/RenderLayer;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/world/AbstractChunkRenderManager;render(Lnet/minecraft/client/render/RenderLayer;)V", shift = At.Shift.BEFORE))
    void impl$renderLayer$pre(RenderLayer layer, CallbackInfo ci) {
        if (Config.isShaders()) ShadersRender.preRenderChunkLayer(layer);
    }

    @Inject(method = "renderLayer(Lnet/minecraft/client/render/RenderLayer;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/world/AbstractChunkRenderManager;render(Lnet/minecraft/client/render/RenderLayer;)V", shift = At.Shift.AFTER))
    void impl$renderLayer$post(RenderLayer layer, CallbackInfo ci) {
        if (Config.isShaders()) ShadersRender.postRenderChunkLayer(layer);
    }

    @Inject(method = "renderSky", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/platform/GlStateManager;disableTexture()V", ordinal = 0))
    void impl$renderSky$begin(CallbackInfo ci) {
        if (shadersEnabled) Shaders.disableTexture2D();
    }

    @Redirect(method = "renderSky", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/world/ClientWorld;method_3631(Lnet/minecraft/entity/Entity;F)Lnet/minecraft/util/math/Vec3d;"))
    Vec3d impl$renderSky$skyColor(ClientWorld instance, Entity entity, float v) {
        Vec3d skyColor = instance.method_3631(entity, v);
        Shaders.setSkyColor(skyColor);
        return skyColor;
    }

    @Redirect(method = "renderSky", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/platform/GlStateManager;color(FFF)V", ordinal = 1))
    void impl$renderSky$fogColor(float red, float green, float blue) {
        if (shadersEnabled) Shaders.enableFog();
        GlStateManager.color(red, green, blue);
        if (shadersEnabled) Shaders.preSkyList();
    }

    @Inject(method = "renderSky", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/platform/GlStateManager;disableFog()V", shift = At.Shift.AFTER))
    void impl$renderSky$disableFog(CallbackInfo ci) {
        if (shadersEnabled) Shaders.disableFog();
    }

    @Inject(method = "renderSky", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/platform/GlStateManager;disableTexture()V", ordinal = 1))
    void impl$renderSky$disableTexture(CallbackInfo ci) {
        if (shadersEnabled) Shaders.disableTexture2D();
    }

    @Inject(method = "renderSky", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/platform/GlStateManager;enableTexture()V", ordinal = 0))
    void impl$renderSky$enableTexture(CallbackInfo ci) {
        if (shadersEnabled) Shaders.enableTexture2D();
    }

    @Redirect(method = "renderSky", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/platform/GlStateManager;rotate(FFFF)V", ordinal = 4))
    void impl$renderSky$rotateCelestial(float angle, float x, float y, float z) {
        if (shadersEnabled) Shaders.preCelestialRotate();
        GlStateManager.rotate(angle, x, y, z);
        if (shadersEnabled) Shaders.postCelestialRotate();
    }

    @Inject(method = "renderSky", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/platform/GlStateManager;disableTexture()V", ordinal = 2))
    void impl$renderSky$disableTexture$2(CallbackInfo ci) {
        if (shadersEnabled) Shaders.disableTexture2D();
    }

    @Inject(method = "renderSky", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/platform/GlStateManager;enableFog()V", ordinal = 1))
    void impl$renderSky$enableFog(CallbackInfo ci) {
        if (shadersEnabled) Shaders.enableFog();
    }

    @Inject(method = "renderSky", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/platform/GlStateManager;disableTexture()V", ordinal = 3))
    void impl$renderSky$disableTexture2D$3(CallbackInfo ci) {
        if (shadersEnabled) Shaders.disableTexture2D();
    }

    @Inject(method = "renderSky", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/platform/GlStateManager;enableTexture()V"))
    void impl$renderSky$enableTexture$1(CallbackInfo ci) {
        if (shadersEnabled) Shaders.enableTexture2D();
    }

    @Inject(method = "renderClouds", at = @At(value = "HEAD"))
    void impl$renderClouds$begin(CallbackInfo ci) {
        if (Config.isShaders()) Shaders.beginClouds();
    }

    @Inject(method = "renderClouds", at = @At(value = "HEAD"))
    void impl$renderClouds$end(CallbackInfo ci) {
        if (Config.isShaders()) Shaders.endClouds();
    }

    @Inject(method = "renderWorldBorder", at = @At(value = "INVOKE", target = "Ljava/lang/Math;pow(DD)D"))
    void impl$renderWorldBorder$beg(Entity entity, float tickDelta, CallbackInfo ci) {
        if (Config.isShaders()) {
            Shaders.pushProgram();
            Shaders.useProgram(Shaders.ProgramTexturedLit);
        }
    }

    @Inject(method = "renderWorldBorder", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/platform/GlStateManager;depthMask(Z)V", ordinal = 1))
    void impl$renderWorldBorder$end(Entity entity, float tickDelta, CallbackInfo ci) {
        if (Config.isShaders()) Shaders.popProgram();
    }

    @Inject(method = "drawBlockOutline", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/platform/GlStateManager;disableTexture()V"))
    void impl$drawBlockOutline$disableTexture(CallbackInfo ci) {
        if (Config.isShaders()) Shaders.disableTexture2D();
    }

    @Inject(method = "drawBlockOutline", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/platform/GlStateManager;enableTexture()V"))
    void impl$drawBlockOutline$enableTexture(CallbackInfo ci) {
        if (Config.isShaders()) Shaders.enableTexture2D();
    }
}

package dev.vexor.photon.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import dev.vexor.photon.Config;
import dev.vexor.photon.tex.ExtendedSpriteAtlasTexture;
import net.minecraft.client.render.TextureStitcher;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.texture.SpriteAtlasTexture;
import net.minecraft.client.texture.TextureUtil;
import net.optifine.shaders.ShadersTex;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(SpriteAtlasTexture.class)
public class MixinSpriteAtlasTexture implements ExtendedSpriteAtlasTexture {
    @Shadow
    private int maxTextureSize;

    @Unique
    private int atlasWidth, atlasHeight;

    @Redirect(method = "method_7005", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/texture/TextureUtil;method_7027([[IIIIIZZ)V"))
    void impl$method_7005(int[][] is, int i, int j, int k, int l, boolean bl, boolean bl2, @Local Sprite sprite3) {
        if (Config.isShaders()) {
            ShadersTex.uploadTexSubForLoadAtlas((SpriteAtlasTexture) (Object) this, sprite3.getName(), sprite3.method_5831(0), sprite3.getWidth(), sprite3.getHeight(), sprite3.getX(), sprite3.getY(), false, false);
        } else {
            TextureUtil.method_7027(sprite3.method_5831(0), sprite3.getWidth(), sprite3.getHeight(), sprite3.getX(), sprite3.getY(), false, false);
        }
    }

    @Redirect(method = "method_7005", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/texture/TextureUtil;prepareImage(IIII)V"))
    void impl$method_7005$prep(int id, int maxLevel, int width, int height, @Local TextureStitcher stitcher) {
        if (Config.isShaders()) {
            ShadersTex.allocateTextureMap(id, this.maxTextureSize, width, height, stitcher, (SpriteAtlasTexture) (Object) this);
        } else {
            TextureUtil.prepareImage(id, maxLevel, width, height);
        }
    }

    @Override
    public int getAtlasWidth() {
        return this.atlasWidth;
    }

    @Override
    public int getAtlasHeight() {
        return this.atlasHeight;
    }

    @Override
    public void setAtlasWidth(int w) {
        this.atlasWidth = w;
    }

    @Override
    public void setAtlasHeight(int h) {
        this.atlasHeight = h;
    }
}
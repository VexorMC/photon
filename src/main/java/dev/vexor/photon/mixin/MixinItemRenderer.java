package dev.vexor.photon.mixin;

import com.mojang.blaze3d.platform.GlStateManager;
import dev.vexor.photon.Config;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.texture.SpriteAtlasTexture;
import net.minecraft.client.texture.TextureManager;
import net.minecraft.util.Identifier;
import net.optifine.shaders.Shaders;
import net.optifine.shaders.ShadersRender;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(ItemRenderer.class)
public abstract class MixinItemRenderer {
    @Shadow @Final private TextureManager textureManager;

    @Shadow @Final private static Identifier ITEM_GLINT_TEXTURE;

    @Shadow protected abstract void renderBakedItemModel(BakedModel model, int color);

    /**
     * @reason Shaders
     * @author Lunasa
     */
    @Overwrite
    void renderGlint(BakedModel model) {
        if (!Config.isShaders() || !Shaders.isShadowPass) {
            GlStateManager.depthMask(false);
            GlStateManager.depthFunc(514);
            GlStateManager.disableLighting();
            GlStateManager.blendFunc(768, 1);
            this.textureManager.bindTexture(ITEM_GLINT_TEXTURE);
            GlStateManager.matrixMode(5890);
            GlStateManager.pushMatrix();
            GlStateManager.scale(8.0F, 8.0F, 8.0F);
            float f = (float) (MinecraftClient.getTime() % 3000L) / 3000.0F / 8.0F;
            GlStateManager.translate(f, 0.0F, 0.0F);
            GlStateManager.rotate(-50.0F, 0.0F, 0.0F, 1.0F);
            this.renderBakedItemModel(model, -8372020);
            GlStateManager.popMatrix();
            GlStateManager.pushMatrix();
            GlStateManager.scale(8.0F, 8.0F, 8.0F);
            float g = (float) (MinecraftClient.getTime() % 4873L) / 4873.0F / 8.0F;
            GlStateManager.translate(-g, 0.0F, 0.0F);
            GlStateManager.rotate(10.0F, 0.0F, 0.0F, 1.0F);
            this.renderBakedItemModel(model, -8372020);
            GlStateManager.popMatrix();
            GlStateManager.matrixMode(5888);
            GlStateManager.blendFunc(770, 771);
            GlStateManager.enableLighting();
            GlStateManager.depthFunc(515);
            GlStateManager.depthMask(true);
            this.textureManager.bindTexture(SpriteAtlasTexture.BLOCK_ATLAS_TEX);
            if (Config.isShaders()) ShadersRender.renderEnchantedGlintEnd();
        }
    }
}

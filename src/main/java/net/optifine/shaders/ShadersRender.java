package net.optifine.shaders;

import com.mojang.blaze3d.platform.GLX;
import com.mojang.blaze3d.platform.GlStateManager;
import java.nio.IntBuffer;

import dev.vexor.photon.Config;
import dev.vexor.photon.tex.ExtendedGameRenderer;
import net.minecraft.block.entity.EndPortalBlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.BaseFrustum;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.CameraView;
import net.minecraft.client.render.CullingCameraView;
import net.minecraft.client.render.DiffuseLighting;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.client.texture.SpriteAtlasTexture;
import net.minecraft.entity.Entity;
import net.minecraft.util.Identifier;
import org.lwjgl.opengl.EXTFramebufferObject;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;

public class ShadersRender {
   private static final Identifier END_PORTAL_TEXTURE = new Identifier("textures/entity/end_portal.png");

   public static void setFrustrumPosition(CameraView frustum, double x, double y, double z) {
      frustum.setPos(x, y, z);
   }

   public static void setupTerrain(
      WorldRenderer renderGlobal, Entity viewEntity, double partialTicks, CameraView camera, int frameCount, boolean playerSpectator
   ) {
      renderGlobal.setupTerrain(viewEntity, partialTicks, camera, frameCount, playerSpectator);
   }

   public static void beginTerrainSolid() {
      if (Shaders.isRenderingWorld) {
         Shaders.fogEnabled = true;
         Shaders.useProgram(Shaders.ProgramTerrain);
      }
   }

   public static void beginTerrainCutoutMipped() {
      if (Shaders.isRenderingWorld) {
         Shaders.useProgram(Shaders.ProgramTerrain);
      }
   }

   public static void beginTerrainCutout() {
      if (Shaders.isRenderingWorld) {
         Shaders.useProgram(Shaders.ProgramTerrain);
      }
   }

   public static void endTerrain() {
      if (Shaders.isRenderingWorld) {
         Shaders.useProgram(Shaders.ProgramTexturedLit);
      }
   }

   public static void beginTranslucent() {
      if (Shaders.isRenderingWorld) {
         if (Shaders.usedDepthBuffers >= 2) {
            GlStateManager.activeTexture(33995);
            Shaders.checkGLError("pre copy depth");
            GL11.glCopyTexSubImage2D(3553, 0, 0, 0, 0, 0, Shaders.renderWidth, Shaders.renderHeight);
            Shaders.checkGLError("copy depth");
            GlStateManager.activeTexture(33984);
         }

         Shaders.useProgram(Shaders.ProgramWater);
      }
   }

   public static void endTranslucent() {
      if (Shaders.isRenderingWorld) {
         Shaders.useProgram(Shaders.ProgramTexturedLit);
      }
   }

   public static void renderHand0(GameRenderer er, float par1, int par2) {
      if (!Shaders.isShadowPass) {
         boolean blockTranslucentMain = Shaders.isItemToRenderMainTranslucent();
         boolean blockTranslucentOff = Shaders.isItemToRenderOffTranslucent();
         if (!blockTranslucentMain || !blockTranslucentOff) {
            Shaders.readCenterDepth();
            Shaders.beginHand(false);
            GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
            Shaders.setSkipRenderHands(blockTranslucentMain, blockTranslucentOff);
            ((ExtendedGameRenderer)er).renderHand(par1, par2, true, false, false);
            Shaders.endHand();
            Shaders.setHandsRendered(!blockTranslucentMain, !blockTranslucentOff);
            Shaders.setSkipRenderHands(false, false);
         }
      }
   }

   public static void renderHand1(GameRenderer er, float par1, int par2) {
      if (!Shaders.isShadowPass && !Shaders.isBothHandsRendered()) {
         Shaders.readCenterDepth();
         GlStateManager.enableBlend();
         Shaders.beginHand(true);
         GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
         Shaders.setSkipRenderHands(Shaders.isHandRenderedMain(), Shaders.isHandRenderedOff());
         ((ExtendedGameRenderer)er).renderHand(par1, par2, true, false, true);
         Shaders.endHand();
         Shaders.setHandsRendered(true, true);
         Shaders.setSkipRenderHands(false, false);
      }
   }

   public static void renderItemFP(HeldItemRenderer itemRenderer, float par1, boolean renderTranslucent) {
      Shaders.setRenderingFirstPersonHand(true);
      GlStateManager.depthMask(true);
      if (renderTranslucent) {
         GlStateManager.depthFunc(519);
         GL11.glPushMatrix();
         IntBuffer drawBuffers = Shaders.activeDrawBuffers;
         Shaders.setDrawBuffers(Shaders.drawBuffersNone);
         Shaders.renderItemKeepDepthMask = true;
         itemRenderer.renderArmHoldingItem(par1);
         Shaders.renderItemKeepDepthMask = false;
         Shaders.setDrawBuffers(drawBuffers);
         GL11.glPopMatrix();
      }

      GlStateManager.depthFunc(515);
      GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
      itemRenderer.renderArmHoldingItem(par1);
      Shaders.setRenderingFirstPersonHand(false);
   }

   public static void renderFPOverlay(GameRenderer er, float par1, int par2) {
      if (!Shaders.isShadowPass) {
         Shaders.beginFPOverlay();
         ((ExtendedGameRenderer)er).renderHand(par1, par2, false, true, false);
         Shaders.endFPOverlay();
      }
   }

   public static void beginBlockDamage() {
      if (Shaders.isRenderingWorld) {
         Shaders.useProgram(Shaders.ProgramDamagedBlock);
         if (Shaders.ProgramDamagedBlock.getId() == Shaders.ProgramTerrain.getId()) {
            Shaders.setDrawBuffers(Shaders.drawBuffersColorAtt0);
            GlStateManager.depthMask(false);
         }
      }
   }

   public static void endBlockDamage() {
      if (Shaders.isRenderingWorld) {
         GlStateManager.depthMask(true);
         Shaders.useProgram(Shaders.ProgramTexturedLit);
      }
   }

   public static void renderShadowMap(GameRenderer entityRenderer, int pass, float partialTicks, long finishTimeNano) {
      if (Shaders.usedShadowDepthBuffers > 0 && --Shaders.shadowPassCounter <= 0) {
         MinecraftClient mc = MinecraftClient.getInstance();
         mc.profiler.swap("shadow pass");
         WorldRenderer renderGlobal = mc.worldRenderer;
         Shaders.isShadowPass = true;
         Shaders.shadowPassCounter = Shaders.shadowPassInterval;
         Shaders.preShadowPassThirdPersonView = mc.options.perspective;
         mc.options.perspective = 1;
         Shaders.checkGLError("pre shadow");
         GL11.glMatrixMode(5889);
         GL11.glPushMatrix();
         GL11.glMatrixMode(5888);
         GL11.glPushMatrix();
         mc.profiler.swap("shadow clear");
         EXTFramebufferObject.glBindFramebufferEXT(36160, Shaders.sfb);
         Shaders.checkGLError("shadow bind sfb");
         mc.profiler.swap("shadow camera");
         entityRenderer.setupCamera(partialTicks, 2);
         Shaders.setCameraShadow(partialTicks);
         Shaders.checkGLError("shadow camera");
         Shaders.useProgram(Shaders.ProgramShadow);
         GL20.glDrawBuffers(Shaders.sfbDrawBuffers);
         Shaders.checkGLError("shadow drawbuffers");
         GL11.glReadBuffer(0);
         Shaders.checkGLError("shadow readbuffer");
         EXTFramebufferObject.glFramebufferTexture2DEXT(36160, 36096, 3553, Shaders.sfbDepthTextures.get(0), 0);
         if (Shaders.usedShadowColorBuffers != 0) {
            EXTFramebufferObject.glFramebufferTexture2DEXT(36160, 36064, 3553, Shaders.sfbColorTextures.get(0), 0);
         }

         Shaders.checkFramebufferStatus("shadow fb");
         GL11.glClearColor(1.0F, 1.0F, 1.0F, 1.0F);
         GL11.glClear(Shaders.usedShadowColorBuffers != 0 ? 16640 : 256);
         Shaders.checkGLError("shadow clear");
         mc.profiler.swap("shadow frustum");
         BaseFrustum clippingHelper = ClippingHelperShadow.getInstance();
         mc.profiler.swap("shadow culling");
         CullingCameraView frustum = new CullingCameraView(clippingHelper);
         Entity viewEntity = mc.getCameraEntity();
         double viewPosX = viewEntity.prevTickX + (viewEntity.x - viewEntity.prevTickX) * (double)partialTicks;
         double viewPosY = viewEntity.prevTickY + (viewEntity.y - viewEntity.prevTickY) * (double)partialTicks;
         double viewPosZ = viewEntity.prevTickZ + (viewEntity.z - viewEntity.prevTickZ) * (double)partialTicks;
         frustum.setPos(viewPosX, viewPosY, viewPosZ);
         GlStateManager.shadeModel(7425);
         GlStateManager.enableDepthTest();
         GlStateManager.depthFunc(515);
         GlStateManager.depthMask(true);
         GlStateManager.colorMask(true, true, true, true);
         GlStateManager.disableCull();
         mc.profiler.swap("shadow prepareterrain");
         mc.getTextureManager().bindTexture(SpriteAtlasTexture.BLOCK_ATLAS_TEX);
         mc.profiler.swap("shadow setupterrain");
         int frameCount = 0;
         frameCount = entityRenderer.frameCount++;
         renderGlobal.setupTerrain(viewEntity, (double)partialTicks, frustum, frameCount, mc.player.isSpectator());
         mc.profiler.swap("shadow updatechunks");
         mc.profiler.swap("shadow terrain");
         GlStateManager.matrixMode(5888);
         GlStateManager.pushMatrix();
         GlStateManager.disableAlphaTest();
         renderGlobal.renderLayer(RenderLayer.SOLID, (double)partialTicks, 2, viewEntity);
         Shaders.checkGLError("shadow terrain solid");
         GlStateManager.enableAlphaTest();
         renderGlobal.renderLayer(RenderLayer.CUTOUT_MIPPED, (double)partialTicks, 2, viewEntity);
         Shaders.checkGLError("shadow terrain cutoutmipped");
         mc.getTextureManager().getTexture(SpriteAtlasTexture.BLOCK_ATLAS_TEX).pushFilter(false, false);
         renderGlobal.renderLayer(RenderLayer.CUTOUT, (double)partialTicks, 2, viewEntity);
         Shaders.checkGLError("shadow terrain cutout");
         mc.getTextureManager().getTexture(SpriteAtlasTexture.BLOCK_ATLAS_TEX).pop();
         GlStateManager.shadeModel(7424);
         GlStateManager.alphaFunc(516, 0.1F);
         GlStateManager.matrixMode(5888);
         GlStateManager.popMatrix();
         GlStateManager.pushMatrix();
         mc.profiler.swap("shadow entities");


         renderGlobal.renderEntities(viewEntity, frustum, partialTicks);
         Shaders.checkGLError("shadow entities");
         GlStateManager.matrixMode(5888);
         GlStateManager.popMatrix();
         GlStateManager.depthMask(true);
         GlStateManager.disableBlend();
         GlStateManager.enableCull();
         GlStateManager.blendFuncSeparate(770, 771, 1, 0);
         GlStateManager.alphaFunc(516, 0.1F);
         if (Shaders.usedShadowDepthBuffers >= 2) {
            GlStateManager.activeTexture(33989);
            Shaders.checkGLError("pre copy shadow depth");
            GL11.glCopyTexSubImage2D(3553, 0, 0, 0, 0, 0, Shaders.shadowMapWidth, Shaders.shadowMapHeight);
            Shaders.checkGLError("copy shadow depth");
            GlStateManager.activeTexture(33984);
         }

         GlStateManager.disableBlend();
         GlStateManager.depthMask(true);
         mc.getTextureManager().bindTexture(SpriteAtlasTexture.BLOCK_ATLAS_TEX);
         GlStateManager.shadeModel(7425);
         Shaders.checkGLError("shadow pre-translucent");
         GL20.glDrawBuffers(Shaders.sfbDrawBuffers);
         Shaders.checkGLError("shadow drawbuffers pre-translucent");
         Shaders.checkFramebufferStatus("shadow pre-translucent");
         if (Shaders.isRenderShadowTranslucent()) {
            mc.profiler.swap("shadow translucent");
            renderGlobal.renderLayer(RenderLayer.TRANSLUCENT, (double)partialTicks, 2, viewEntity);
            Shaders.checkGLError("shadow translucent");
         }


         GlStateManager.shadeModel(7424);
         GlStateManager.depthMask(true);
         GlStateManager.enableCull();
         GlStateManager.disableBlend();
         GL11.glFlush();
         Shaders.checkGLError("shadow flush");
         Shaders.isShadowPass = false;
         mc.options.perspective = Shaders.preShadowPassThirdPersonView;
         mc.profiler.swap("shadow postprocess");
         if (Shaders.hasGlGenMipmap) {
            if (Shaders.usedShadowDepthBuffers >= 1) {
               if (Shaders.shadowMipmapEnabled[0]) {
                  GlStateManager.activeTexture(33988);
                  GlStateManager.bindTexture(Shaders.sfbDepthTextures.get(0));
                  GL30.glGenerateMipmap(3553);
                  GL11.glTexParameteri(3553, 10241, Shaders.shadowFilterNearest[0] ? 9984 : 9987);
               }

               if (Shaders.usedShadowDepthBuffers >= 2 && Shaders.shadowMipmapEnabled[1]) {
                  GlStateManager.activeTexture(33989);
                  GlStateManager.bindTexture(Shaders.sfbDepthTextures.get(1));
                  GL30.glGenerateMipmap(3553);
                  GL11.glTexParameteri(3553, 10241, Shaders.shadowFilterNearest[1] ? 9984 : 9987);
               }

               GlStateManager.activeTexture(33984);
            }

            if (Shaders.usedShadowColorBuffers >= 1) {
               if (Shaders.shadowColorMipmapEnabled[0]) {
                  GlStateManager.activeTexture(33997);
                  GlStateManager.bindTexture(Shaders.sfbColorTextures.get(0));
                  GL30.glGenerateMipmap(3553);
                  GL11.glTexParameteri(3553, 10241, Shaders.shadowColorFilterNearest[0] ? 9984 : 9987);
               }

               if (Shaders.usedShadowColorBuffers >= 2 && Shaders.shadowColorMipmapEnabled[1]) {
                  GlStateManager.activeTexture(33998);
                  GlStateManager.bindTexture(Shaders.sfbColorTextures.get(1));
                  GL30.glGenerateMipmap(3553);
                  GL11.glTexParameteri(3553, 10241, Shaders.shadowColorFilterNearest[1] ? 9984 : 9987);
               }

               GlStateManager.activeTexture(33984);
            }
         }

         Shaders.checkGLError("shadow postprocess");
         EXTFramebufferObject.glBindFramebufferEXT(36160, Shaders.dfb);
         GL11.glViewport(0, 0, Shaders.renderWidth, Shaders.renderHeight);
         Shaders.activeDrawBuffers = null;
         mc.getTextureManager().bindTexture(SpriteAtlasTexture.BLOCK_ATLAS_TEX);
         Shaders.useProgram(Shaders.ProgramTerrain);
         GL11.glMatrixMode(5888);
         GL11.glPopMatrix();
         GL11.glMatrixMode(5889);
         GL11.glPopMatrix();
         GL11.glMatrixMode(5888);
         Shaders.checkGLError("shadow end");
      }
   }

   public static void preRenderChunkLayer(RenderLayer blockLayerIn) {
      if (Shaders.isRenderBackFace(blockLayerIn)) {
         GlStateManager.disableCull();
      }

      if (GLX.supportsVbo()) {
         GL11.glEnableClientState(32885);
         GL20.glEnableVertexAttribArray(Shaders.midTexCoordAttrib);
         GL20.glEnableVertexAttribArray(Shaders.tangentAttrib);
         GL20.glEnableVertexAttribArray(Shaders.entityAttrib);
      }
   }

   public static void postRenderChunkLayer(RenderLayer blockLayerIn) {
      if (GLX.supportsVbo()) {
         GL11.glDisableClientState(32885);
         GL20.glDisableVertexAttribArray(Shaders.midTexCoordAttrib);
         GL20.glDisableVertexAttribArray(Shaders.tangentAttrib);
         GL20.glDisableVertexAttribArray(Shaders.entityAttrib);
      }

      if (Shaders.isRenderBackFace(blockLayerIn)) {
         GlStateManager.enableCull();
      }
   }

   public static void setupArrayPointersVbo() {
      int vertexSizeI = 14;
      GL11.glVertexPointer(3, 5126, 56, 0L);
      GL11.glColorPointer(4, 5121, 56, 12L);
      GL11.glTexCoordPointer(2, 5126, 56, 16L);
      GLX.gl13ClientActiveTexture(GLX.lightmapTextureUnit);
      GL11.glTexCoordPointer(2, 5122, 56, 24L);
      GLX.gl13ClientActiveTexture(GLX.textureUnit);
      GL11.glNormalPointer(5120, 56, 28L);
      GL20.glVertexAttribPointer(Shaders.midTexCoordAttrib, 2, 5126, false, 56, 32L);
      GL20.glVertexAttribPointer(Shaders.tangentAttrib, 4, 5122, false, 56, 40L);
      GL20.glVertexAttribPointer(Shaders.entityAttrib, 3, 5122, false, 56, 48L);
   }

   public static void beaconBeamBegin() {
      Shaders.useProgram(Shaders.ProgramBeaconBeam);
   }

   public static void beaconBeamStartQuad1() {
   }

   public static void beaconBeamStartQuad2() {
   }

   public static void beaconBeamDraw1() {
   }

   public static void beaconBeamDraw2() {
      GlStateManager.disableBlend();
   }

   public static void renderEnchantedGlintBegin() {
      Shaders.useProgram(Shaders.ProgramArmorGlint);
   }

   public static void renderEnchantedGlintEnd() {
      if (Shaders.isRenderingWorld) {
         if (Shaders.isRenderingFirstPersonHand() && Shaders.isRenderBothHands()) {
            Shaders.useProgram(Shaders.ProgramHand);
         } else {
            Shaders.useProgram(Shaders.ProgramEntities);
         }
      } else {
         Shaders.useProgram(Shaders.ProgramNone);
      }
   }

   public static boolean renderEndPortal(EndPortalBlockEntity te, double x, double y, double z, float partialTicks, int destroyStage, float offset) {
      if (!Shaders.isShadowPass && Shaders.activeProgram.getId() == 0) {
         return false;
      } else {
         GlStateManager.disableLighting();
         Config.getTextureManager().bindTexture(END_PORTAL_TEXTURE);
         Tessellator tessellator = Tessellator.getInstance();
         BufferBuilder vertexbuffer = tessellator.getBuffer();
         vertexbuffer.begin(7, VertexFormats.BLOCK);
         float col = 0.5F;
         float r = col * 0.15F;
         float g = col * 0.3F;
         float b = col * 0.4F;
         float u0 = 0.0F;
         float u1 = 0.2F;
         float du = (float)(System.currentTimeMillis() % 100000L) / 100000.0F;
         int lu = 240;
         vertexbuffer.vertex(x, y + (double)offset, z + 1.0).color(r, g, b, 1.0F).texture((double)(u0 + du), (double)(u0 + du)).texture2(lu, lu).next();
         vertexbuffer.vertex(x + 1.0, y + (double)offset, z + 1.0).color(r, g, b, 1.0F).texture((double)(u0 + du), (double)(u1 + du)).texture2(lu, lu).next();
         vertexbuffer.vertex(x + 1.0, y + (double)offset, z).color(r, g, b, 1.0F).texture((double)(u1 + du), (double)(u1 + du)).texture2(lu, lu).next();
         vertexbuffer.vertex(x, y + (double)offset, z).color(r, g, b, 1.0F).texture((double)(u1 + du), (double)(u0 + du)).texture2(lu, lu).next();
         tessellator.draw();
         GlStateManager.enableLighting();
         return true;
      }
   }
}

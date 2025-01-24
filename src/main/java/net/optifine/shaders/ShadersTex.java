package net.optifine.shaders;

import com.mojang.blaze3d.platform.GLX;
import com.mojang.blaze3d.platform.GlStateManager;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.imageio.ImageIO;

import dev.vexor.photon.Config;
import dev.vexor.photon.tex.ExtendedSpriteAtlasTexture;
import dev.vexor.photon.tex.ExtendedTexture;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.TextureStitcher;
import net.minecraft.client.texture.AbstractTexture;
import net.minecraft.client.texture.LayeredTexture;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.texture.SpriteAtlasTexture;
import net.minecraft.client.texture.Texture;
import net.minecraft.client.texture.TextureManager;
import net.minecraft.client.texture.TextureUtil;
import net.minecraft.resource.Resource;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;

public class ShadersTex {
   public static final int initialBufferSize = 1048576;
   public static ByteBuffer byteBuffer = BufferUtils.createByteBuffer(4194304);
   public static IntBuffer intBuffer = byteBuffer.asIntBuffer();
   public static int[] intArray = new int[1048576];
   public static final int defBaseTexColor = 0;
   public static final int defNormTexColor = -8421377;
   public static final int defSpecTexColor = 0;
   public static Map<Integer, MultiTexID> multiTexMap = new HashMap<>();

   public static IntBuffer getIntBuffer(int size) {
      if (intBuffer.capacity() < size) {
         int bufferSize = roundUpPOT(size);
         byteBuffer = BufferUtils.createByteBuffer(bufferSize * 4);
         intBuffer = byteBuffer.asIntBuffer();
      }

      return intBuffer;
   }

   public static int[] getIntArray(int size) {
      if (intArray == null) {
         intArray = new int[1048576];
      }

      if (intArray.length < size) {
         intArray = new int[roundUpPOT(size)];
      }

      return intArray;
   }

   public static int roundUpPOT(int x) {
      int i = x - 1;
      i |= i >> 1;
      i |= i >> 2;
      i |= i >> 4;
      i |= i >> 8;
      i |= i >> 16;
      return i + 1;
   }

   public static int log2(int x) {
      int log = 0;
      if ((x & -65536) != 0) {
         log += 16;
         x >>= 16;
      }

      if ((x & 0xFF00) != 0) {
         log += 8;
         x >>= 8;
      }

      if ((x & 240) != 0) {
         log += 4;
         x >>= 4;
      }

      if ((x & 6) != 0) {
         log += 2;
         x >>= 2;
      }

      if ((x & 2) != 0) {
         log++;
      }

      return log;
   }

   public static IntBuffer fillIntBuffer(int size, int value) {
      int[] aint = getIntArray(size);
      IntBuffer intBuf = getIntBuffer(size);
      Arrays.fill(intArray, 0, size, value);
      intBuffer.put(intArray, 0, size);
      return intBuffer;
   }

   public static int[] createAIntImage(int size) {
      int[] aint = new int[size * 3];
      Arrays.fill(aint, 0, size, 0);
      Arrays.fill(aint, size, size * 2, -8421377);
      Arrays.fill(aint, size * 2, size * 3, 0);
      return aint;
   }

   public static int[] createAIntImage(int size, int color) {
      int[] aint = new int[size * 3];
      Arrays.fill(aint, 0, size, color);
      Arrays.fill(aint, size, size * 2, -8421377);
      Arrays.fill(aint, size * 2, size * 3, 0);
      return aint;
   }

   public static MultiTexID getMultiTexID(AbstractTexture tex) {
      MultiTexID multiTex = ((ExtendedTexture)tex).getMultiTexDirect();
      if (multiTex == null) {
         int baseTex = tex.getGlId();
         multiTex = multiTexMap.get(baseTex);
         if (multiTex == null) {
            multiTex = new MultiTexID(baseTex, GL11.glGenTextures(), GL11.glGenTextures());
            multiTexMap.put(baseTex, multiTex);
         }

         ((ExtendedTexture)tex).setMultiTex(multiTex);
      }

      return multiTex;
   }

   public static void deleteTextures(AbstractTexture atex, int texid) {
      MultiTexID multiTex = ((ExtendedTexture)atex).getMultiTexDirect();
      if (multiTex != null) {
         ((ExtendedTexture)atex).setMultiTex(multiTex);
         multiTexMap.remove(multiTex.base);
         GlStateManager.deleteTexture(multiTex.norm);
         GlStateManager.deleteTexture(multiTex.spec);
         if (multiTex.base != texid) {
            SMCLog.warning("Error : MultiTexID.base mismatch: " + multiTex.base + ", texid: " + texid);
            GlStateManager.deleteTexture(multiTex.base);
         }
      }
   }

   public static void bindNSTextures(int normTex, int specTex) {
      if (Shaders.isRenderingWorld && GlStateManager.activeTexture + GLX.textureUnit == 33984) {
         GlStateManager.activeTexture(33986);
         GlStateManager.bindTexture(normTex);
         GlStateManager.activeTexture(33987);
         GlStateManager.bindTexture(specTex);
         GlStateManager.activeTexture(33984);
      }
   }

   public static void bindNSTextures(MultiTexID multiTex) {
      bindNSTextures(multiTex.norm, multiTex.spec);
   }

   public static void bindTextures(int baseTex, int normTex, int specTex) {
      if (Shaders.isRenderingWorld && GlStateManager.activeTexture + GLX.textureUnit == 33984) {
         GlStateManager.activeTexture(33986);
         GlStateManager.bindTexture(normTex);
         GlStateManager.activeTexture(33987);
         GlStateManager.bindTexture(specTex);
         GlStateManager.activeTexture(33984);
      }

      GlStateManager.bindTexture(baseTex);
   }

   public static void bindTextures(MultiTexID multiTex) {
      if (Shaders.isRenderingWorld && GlStateManager.activeTexture + GLX.textureUnit == 33984) {
         if (Shaders.configNormalMap) {
            GlStateManager.activeTexture(33986);
            GlStateManager.bindTexture(multiTex.norm);
         }

         if (Shaders.configSpecularMap) {
            GlStateManager.activeTexture(33987);
            GlStateManager.bindTexture(multiTex.spec);
         }

         GlStateManager.activeTexture(33984);
      }

      GlStateManager.bindTexture(multiTex.base);
   }

   public static void bindTexture(Texture tex) {
      int texId = tex.getGlId();
      bindTextures(((ExtendedTexture)tex).getMultiTex());
      if (GlStateManager.activeTexture + GLX.textureUnit == 33984) {
         int prevSizeX = Shaders.atlasSizeX;
         int prevSizeY = Shaders.atlasSizeY;
         if (tex instanceof SpriteAtlasTexture) {
            Shaders.atlasSizeX = ((ExtendedSpriteAtlasTexture)tex).getAtlasWidth();
            Shaders.atlasSizeY = ((ExtendedSpriteAtlasTexture)tex).getAtlasHeight();
         } else {
            Shaders.atlasSizeX = 0;
            Shaders.atlasSizeY = 0;
         }

         if (Shaders.atlasSizeX != prevSizeX || Shaders.atlasSizeY != prevSizeY) {
            Shaders.uniform_atlasSize.setValue(Shaders.atlasSizeX, Shaders.atlasSizeY);
         }
      }
   }

   public static void bindTextures(int baseTex) {
      MultiTexID multiTex = multiTexMap.get(baseTex);
      bindTextures(multiTex);
   }

   public static void initDynamicTexture(int texID, int width, int height, NativeImageBackedTexture tex) {
      MultiTexID multiTex = ((ExtendedTexture)tex).getMultiTex();
      int[] aint = tex.getPixels();
      int size = width * height;
      Arrays.fill(aint, size, size * 2, -8421377);
      Arrays.fill(aint, size * 2, size * 3, 0);
      TextureUtil.prepareImage(multiTex.base, width, height);
      TextureUtil.setTextureScaling(false, false);
      TextureUtil.setTextureWrapping(false);
      TextureUtil.prepareImage(multiTex.norm, width, height);
      TextureUtil.setTextureScaling(false, false);
      TextureUtil.setTextureWrapping(false);
      TextureUtil.prepareImage(multiTex.spec, width, height);
      TextureUtil.setTextureScaling(false, false);
      TextureUtil.setTextureWrapping(false);
      GlStateManager.bindTexture(multiTex.base);
   }

   public static void updateDynamicTexture(int texID, int[] src, int width, int height, NativeImageBackedTexture tex) {
       MultiTexID multiTex = ((ExtendedTexture)tex).getMultiTex();
      GlStateManager.bindTexture(multiTex.base);
      updateDynTexSubImage1(src, width, height, 0, 0, 0);
      GlStateManager.bindTexture(multiTex.norm);
      updateDynTexSubImage1(src, width, height, 0, 0, 1);
      GlStateManager.bindTexture(multiTex.spec);
      updateDynTexSubImage1(src, width, height, 0, 0, 2);
      GlStateManager.bindTexture(multiTex.base);
   }

   public static void updateDynTexSubImage1(int[] src, int width, int height, int posX, int posY, int page) {
      int size = width * height;
      IntBuffer intBuf = getIntBuffer(size);
      ((Buffer)intBuf).clear();
      int offset = page * size;
      if (src.length >= offset + size) {
         ((Buffer)intBuf.put(src, offset, size)).position(0).limit(size);
         GL11.glTexSubImage2D(3553, 0, posX, posY, width, height, 32993, 33639, intBuf);
         ((Buffer)intBuf).clear();
      }
   }

   public static Texture createDefaultTexture() {
      NativeImageBackedTexture tex = new NativeImageBackedTexture(1, 1);
      tex.getPixels()[0] = -1;
      tex.upload();
      return tex;
   }

   public static void allocateTextureMap(int texID, int mipmapLevels, int width, int height, TextureStitcher stitcher, SpriteAtlasTexture tex) {
      SMCLog.info("allocateTextureMap " + mipmapLevels + " " + width + " " + height + " ");
      ((ExtendedSpriteAtlasTexture)tex).setAtlasWidth(width);
      ((ExtendedSpriteAtlasTexture)tex).setAtlasHeight(height);
      MultiTexID multiTex = getMultiTexID(tex);
      TextureUtil.prepareImage(multiTex.base, mipmapLevels, width, height);
      if (Shaders.configNormalMap) {
         TextureUtil.prepareImage(multiTex.norm, mipmapLevels, width, height);
      }

      if (Shaders.configSpecularMap) {
         TextureUtil.prepareImage(multiTex.spec, mipmapLevels, width, height);
      }

      GlStateManager.bindTexture(texID);
   }

   public static void uploadTexSubForLoadAtlas(
      SpriteAtlasTexture textureMap, String iconName, int[][] data, int width, int height, int xoffset, int yoffset, boolean linear, boolean clamp
   ) {
      MultiTexID updatingTex = ((ExtendedTexture)textureMap).getMultiTex();
      TextureUtil.method_7027(data, width, height, xoffset, yoffset, linear, clamp);
      boolean border = false;
      if (Shaders.configNormalMap) {
         int[][] aaint = readImageAndMipmaps(textureMap, iconName + "_n", width, height, data.length, border, -8421377);
         GlStateManager.bindTexture(updatingTex.norm);
         TextureUtil.method_7027(aaint, width, height, xoffset, yoffset, linear, clamp);
      }

      if (Shaders.configSpecularMap) {
         int[][] aaint = readImageAndMipmaps(textureMap, iconName + "_s", width, height, data.length, border, 0);
         GlStateManager.bindTexture(updatingTex.spec);
         TextureUtil.method_7027(aaint, width, height, xoffset, yoffset, linear, clamp);
      }

      GlStateManager.bindTexture(updatingTex.base);
   }

   public static int[][] readImageAndMipmaps(
      SpriteAtlasTexture updatingTextureMap, String name, int width, int height, int numLevels, boolean border, int defColor
   ) {
      MultiTexID updatingTex = ((ExtendedTexture)updatingTextureMap).getMultiTex();
      int[][] aaint = new int[numLevels][];
      int[] aint;
      aaint[0] = aint = new int[width * height];
      boolean goodImage = false;
      BufferedImage image = readImage(updatingTextureMap.method_7003(new Identifier(name), 0));
      if (image != null) {
         int imageWidth = image.getWidth();
         int imageHeight = image.getHeight();
         if (imageWidth + (border ? 16 : 0) == width) {
            goodImage = true;
            image.getRGB(0, 0, imageWidth, imageWidth, aint, 0, imageWidth);
         }
      }

      if (!goodImage) {
         Arrays.fill(aint, defColor);
      }

      GlStateManager.bindTexture(updatingTex.spec);
      return genMipmapsSimple(aaint.length - 1, width, aaint);
   }

   public static BufferedImage readImage(Identifier resLoc) {
      try {
         if (!Config.hasResource(resLoc)) {
            return null;
         } else {
            InputStream istr = Config.getResourceStream(resLoc);
            if (istr == null) {
               return null;
            } else {
               BufferedImage image = ImageIO.read(istr);
               istr.close();
               return image;
            }
         }
      } catch (IOException var3) {
         return null;
      }
   }

   public static int[][] genMipmapsSimple(int maxLevel, int width, int[][] data) {
      for (int level = 1; level <= maxLevel; level++) {
         if (data[level] == null) {
            int cw = width >> level;
            int pw = cw * 2;
            int[] aintp = data[level - 1];
            int[] aintc = data[level] = new int[cw * cw];

            for (int y = 0; y < cw; y++) {
               for (int x = 0; x < cw; x++) {
                  int ppos = y * 2 * pw + x * 2;
                  aintc[y * cw + x] = blend4Simple(aintp[ppos], aintp[ppos + 1], aintp[ppos + pw], aintp[ppos + pw + 1]);
               }
            }
         }
      }

      return data;
   }

   public static void uploadTexSub1(int[][] src, int width, int height, int posX, int posY, int page) {
      int size = width * height;
      IntBuffer intBuf = getIntBuffer(size);
      int numLevel = src.length;
      int level = 0;
      int lw = width;
      int lh = height;
      int px = posX;

      for (int py = posY; lw > 0 && lh > 0 && level < numLevel; level++) {
         int lsize = lw * lh;
         int[] aint = src[level];
         ((Buffer)intBuf).clear();
         if (aint.length >= lsize * (page + 1)) {
            ((Buffer)intBuf.put(aint, lsize * page, lsize)).position(0).limit(lsize);
            GL11.glTexSubImage2D(3553, level, px, py, lw, lh, 32993, 33639, intBuf);
         }

         lw >>= 1;
         lh >>= 1;
         px >>= 1;
         py >>= 1;
      }

      ((Buffer)intBuf).clear();
   }

   public static int blend4Alpha(int c0, int c1, int c2, int c3) {
      int a0 = c0 >>> 24 & 0xFF;
      int a1 = c1 >>> 24 & 0xFF;
      int a2 = c2 >>> 24 & 0xFF;
      int a3 = c3 >>> 24 & 0xFF;
      int as = a0 + a1 + a2 + a3;
      int an = (as + 2) / 4;
      int dv;
      if (as != 0) {
         dv = as;
      } else {
         dv = 4;
         a0 = 1;
         a1 = 1;
         a2 = 1;
         a3 = 1;
      }

      int frac = (dv + 1) / 2;
      return an << 24
         | ((c0 >>> 16 & 0xFF) * a0 + (c1 >>> 16 & 0xFF) * a1 + (c2 >>> 16 & 0xFF) * a2 + (c3 >>> 16 & 0xFF) * a3 + frac) / dv << 16
         | ((c0 >>> 8 & 0xFF) * a0 + (c1 >>> 8 & 0xFF) * a1 + (c2 >>> 8 & 0xFF) * a2 + (c3 >>> 8 & 0xFF) * a3 + frac) / dv << 8
         | ((c0 >>> 0 & 0xFF) * a0 + (c1 >>> 0 & 0xFF) * a1 + (c2 >>> 0 & 0xFF) * a2 + (c3 >>> 0 & 0xFF) * a3 + frac) / dv << 0;
   }

   public static int blend4Simple(int c0, int c1, int c2, int c3) {
      return ((c0 >>> 24 & 0xFF) + (c1 >>> 24 & 0xFF) + (c2 >>> 24 & 0xFF) + (c3 >>> 24 & 0xFF) + 2) / 4 << 24
         | ((c0 >>> 16 & 0xFF) + (c1 >>> 16 & 0xFF) + (c2 >>> 16 & 0xFF) + (c3 >>> 16 & 0xFF) + 2) / 4 << 16
         | ((c0 >>> 8 & 0xFF) + (c1 >>> 8 & 0xFF) + (c2 >>> 8 & 0xFF) + (c3 >>> 8 & 0xFF) + 2) / 4 << 8
         | ((c0 >>> 0 & 0xFF) + (c1 >>> 0 & 0xFF) + (c2 >>> 0 & 0xFF) + (c3 >>> 0 & 0xFF) + 2) / 4 << 0;
   }

   public static void genMipmapAlpha(int[] aint, int offset, int width, int height) {
      int minwh = Math.min(width, height);
      int o2 = offset;
      int w2 = width;
      int h2 = height;
      int o1 = 0;
      int w1 = 0;
      int h1 = 0;

      int level;
      for (level = 0; w2 > 1 && h2 > 1; o2 = o1) {
         o1 = o2 + w2 * h2;
         w1 = w2 / 2;
         h1 = h2 / 2;

         for (int y = 0; y < h1; y++) {
            int p1 = o1 + y * w1;
            int p2 = o2 + y * 2 * w2;

            for (int x = 0; x < w1; x++) {
               aint[p1 + x] = blend4Alpha(aint[p2 + x * 2], aint[p2 + x * 2 + 1], aint[p2 + w2 + x * 2], aint[p2 + w2 + x * 2 + 1]);
            }
         }

         level++;
         w2 = w1;
         h2 = h1;
      }

      while (level > 0) {
         w2 = width >> --level;
         h2 = height >> level;
         o2 = o1 - w2 * h2;
         int p2 = o2;

         for (int y = 0; y < h2; y++) {
            for (int x = 0; x < w2; x++) {
               if (aint[p2] == 0) {
                  aint[p2] = aint[o1 + y / 2 * w1 + x / 2] & 16777215;
               }

               p2++;
            }
         }

         o1 = o2;
         w1 = w2;
      }
   }

   public static void genMipmapSimple(int[] aint, int offset, int width, int height) {
      int minwh = Math.min(width, height);
      int o2 = offset;
      int w2 = width;
      int h2 = height;
      int o1 = 0;
      int w1 = 0;
      int h1 = 0;

      int level;
      for (level = 0; w2 > 1 && h2 > 1; o2 = o1) {
         o1 = o2 + w2 * h2;
         w1 = w2 / 2;
         h1 = h2 / 2;

         for (int y = 0; y < h1; y++) {
            int p1 = o1 + y * w1;
            int p2 = o2 + y * 2 * w2;

            for (int x = 0; x < w1; x++) {
               aint[p1 + x] = blend4Simple(aint[p2 + x * 2], aint[p2 + x * 2 + 1], aint[p2 + w2 + x * 2], aint[p2 + w2 + x * 2 + 1]);
            }
         }

         level++;
         w2 = w1;
         h2 = h1;
      }

      while (level > 0) {
         w2 = width >> --level;
         h2 = height >> level;
         o2 = o1 - w2 * h2;
         int p2 = o2;

         for (int y = 0; y < h2; y++) {
            for (int x = 0; x < w2; x++) {
               if (aint[p2] == 0) {
                  aint[p2] = aint[o1 + y / 2 * w1 + x / 2] & 16777215;
               }

               p2++;
            }
         }

         o1 = o2;
         w1 = w2;
      }
   }

   public static boolean isSemiTransparent(int[] aint, int width, int height) {
      int size = width * height;
      if (aint[0] >>> 24 == 255 && aint[size - 1] == 0) {
         return true;
      } else {
         for (int i = 0; i < size; i++) {
            int alpha = aint[i] >>> 24;
            if (alpha != 0 && alpha != 255) {
               return true;
            }
         }

         return false;
      }
   }

   public static void updateSubTex1(int[] src, int width, int height, int posX, int posY) {
      int level = 0;
      int cw = width;
      int ch = height;
      int cx = posX;

      for (int cy = posY; cw > 0 && ch > 0; cy /= 2) {
         GL11.glCopyTexSubImage2D(3553, level, cx, cy, 0, 0, cw, ch);
         level++;
         cw /= 2;
         ch /= 2;
         cx /= 2;
      }
   }

   public static void setupTexture(MultiTexID multiTex, int[] src, int width, int height, boolean linear, boolean clamp) {
      int mmfilter = linear ? 9729 : 9728;
      int wraptype = clamp ? '脯' : 10497;
      int size = width * height;
      IntBuffer intBuf = getIntBuffer(size);
      ((Buffer)intBuf).clear();
      ((Buffer)intBuf.put(src, 0, size)).position(0).limit(size);
      GlStateManager.bindTexture(multiTex.base);
      GL11.glTexImage2D(3553, 0, 6408, width, height, 0, 32993, 33639, intBuf);
      GL11.glTexParameteri(3553, 10241, mmfilter);
      GL11.glTexParameteri(3553, 10240, mmfilter);
      GL11.glTexParameteri(3553, 10242, wraptype);
      GL11.glTexParameteri(3553, 10243, wraptype);
      ((Buffer)intBuf.put(src, size, size)).position(0).limit(size);
      GlStateManager.bindTexture(multiTex.norm);
      GL11.glTexImage2D(3553, 0, 6408, width, height, 0, 32993, 33639, intBuf);
      GL11.glTexParameteri(3553, 10241, mmfilter);
      GL11.glTexParameteri(3553, 10240, mmfilter);
      GL11.glTexParameteri(3553, 10242, wraptype);
      GL11.glTexParameteri(3553, 10243, wraptype);
      ((Buffer)intBuf.put(src, size * 2, size)).position(0).limit(size);
      GlStateManager.bindTexture(multiTex.spec);
      GL11.glTexImage2D(3553, 0, 6408, width, height, 0, 32993, 33639, intBuf);
      GL11.glTexParameteri(3553, 10241, mmfilter);
      GL11.glTexParameteri(3553, 10240, mmfilter);
      GL11.glTexParameteri(3553, 10242, wraptype);
      GL11.glTexParameteri(3553, 10243, wraptype);
      GlStateManager.bindTexture(multiTex.base);
   }

   public static void updateSubImage(MultiTexID multiTex, int[] src, int width, int height, int posX, int posY, boolean linear, boolean clamp) {
      int size = width * height;
      IntBuffer intBuf = getIntBuffer(size);
      ((Buffer)intBuf).clear();
      intBuf.put(src, 0, size);
      ((Buffer)intBuf).position(0).limit(size);
      GlStateManager.bindTexture(multiTex.base);
      GL11.glTexParameteri(3553, 10241, 9728);
      GL11.glTexParameteri(3553, 10240, 9728);
      GL11.glTexParameteri(3553, 10242, 10497);
      GL11.glTexParameteri(3553, 10243, 10497);
      GL11.glTexSubImage2D(3553, 0, posX, posY, width, height, 32993, 33639, intBuf);
      if (src.length == size * 3) {
         ((Buffer)intBuf).clear();
         ((Buffer)intBuf.put(src, size, size)).position(0);
         ((Buffer)intBuf).position(0).limit(size);
      }

      GlStateManager.bindTexture(multiTex.norm);
      GL11.glTexParameteri(3553, 10241, 9728);
      GL11.glTexParameteri(3553, 10240, 9728);
      GL11.glTexParameteri(3553, 10242, 10497);
      GL11.glTexParameteri(3553, 10243, 10497);
      GL11.glTexSubImage2D(3553, 0, posX, posY, width, height, 32993, 33639, intBuf);
      if (src.length == size * 3) {
         ((Buffer)intBuf).clear();
         intBuf.put(src, size * 2, size);
         ((Buffer)intBuf).position(0).limit(size);
      }

      GlStateManager.bindTexture(multiTex.spec);
      GL11.glTexParameteri(3553, 10241, 9728);
      GL11.glTexParameteri(3553, 10240, 9728);
      GL11.glTexParameteri(3553, 10242, 10497);
      GL11.glTexParameteri(3553, 10243, 10497);
      GL11.glTexSubImage2D(3553, 0, posX, posY, width, height, 32993, 33639, intBuf);
      GlStateManager.activeTexture(33984);
   }

   public static Identifier getNSMapLocation(Identifier location, String mapName) {
      if (location == null) {
         return null;
      } else {
         String basename = location.getPath();
         String[] basenameParts = basename.split(".png");
         String basenameNoFileType = basenameParts[0];
         return new Identifier(location.getNamespace(), basenameNoFileType + "_" + mapName + ".png");
      }
   }

   public static void loadNSMap(ResourceManager manager, Identifier location, int width, int height, int[] aint) {
      if (Shaders.configNormalMap) {
         loadNSMap1(manager, getNSMapLocation(location, "n"), width, height, aint, width * height, -8421377);
      }

      if (Shaders.configSpecularMap) {
         loadNSMap1(manager, getNSMapLocation(location, "s"), width, height, aint, width * height * 2, 0);
      }
   }

   private static void loadNSMap1(ResourceManager manager, Identifier location, int width, int height, int[] aint, int offset, int defaultColor) {
      if (!loadNSMapFile(manager, location, width, height, aint, offset)) {
         Arrays.fill(aint, offset, offset + width * height, defaultColor);
      }
   }

   private static boolean loadNSMapFile(ResourceManager manager, Identifier location, int width, int height, int[] aint, int offset) {
      if (location == null) {
         return false;
      } else {
         try {
            Resource res = manager.getResource(location);
            BufferedImage bufferedimage = ImageIO.read(res.getInputStream());
            if (bufferedimage == null) {
               return false;
            } else if (bufferedimage.getWidth() == width && bufferedimage.getHeight() == height) {
               bufferedimage.getRGB(0, 0, width, height, aint, offset, width);
               return true;
            } else {
               return false;
            }
         } catch (IOException var8) {
            return false;
         }
      }
   }

   public static int loadSimpleTexture(
      int textureID, BufferedImage bufferedimage, boolean linear, boolean clamp, ResourceManager resourceManager, Identifier location, MultiTexID multiTex
   ) {
      int width = bufferedimage.getWidth();
      int height = bufferedimage.getHeight();
      int size = width * height;
      int[] aint = getIntArray(size * 3);
      bufferedimage.getRGB(0, 0, width, height, aint, 0, width);
      loadNSMap(resourceManager, location, width, height, aint);
      setupTexture(multiTex, aint, width, height, linear, clamp);
      return textureID;
   }

   public static void mergeImage(int[] aint, int dstoff, int srcoff, int size) {
   }

   public static int blendColor(int color1, int color2, int factor1) {
      int factor2 = 255 - factor1;
      return ((color1 >>> 24 & 0xFF) * factor1 + (color2 >>> 24 & 0xFF) * factor2) / 255 << 24
         | ((color1 >>> 16 & 0xFF) * factor1 + (color2 >>> 16 & 0xFF) * factor2) / 255 << 16
         | ((color1 >>> 8 & 0xFF) * factor1 + (color2 >>> 8 & 0xFF) * factor2) / 255 << 8
         | ((color1 >>> 0 & 0xFF) * factor1 + (color2 >>> 0 & 0xFF) * factor2) / 255 << 0;
   }

   public static void loadLayeredTexture(LayeredTexture tex, ResourceManager manager, List<String> list) {
      int width = 0;
      int height = 0;
      int size = 0;
      int[] image = null;

      for (String s : list) {
         if (s != null) {
            try {
               Identifier location = new Identifier(s);
               InputStream inputstream = manager.getResource(location).getInputStream();
               BufferedImage bufimg = ImageIO.read(inputstream);
               if (size == 0) {
                  width = bufimg.getWidth();
                  height = bufimg.getHeight();
                  size = width * height;
                  image = createAIntImage(size, 0);
               }

               int[] aint = getIntArray(size * 3);
               bufimg.getRGB(0, 0, width, height, aint, 0, width);
               loadNSMap(manager, location, width, height, aint);

               for (int i = 0; i < size; i++) {
                  int alpha = aint[i] >>> 24 & 0xFF;
                  image[size * 0 + i] = blendColor(aint[size * 0 + i], image[size * 0 + i], alpha);
                  image[size * 1 + i] = blendColor(aint[size * 1 + i], image[size * 1 + i], alpha);
                  image[size * 2 + i] = blendColor(aint[size * 2 + i], image[size * 2 + i], alpha);
               }
            } catch (IOException var15) {
               var15.printStackTrace();
            }
         }
      }

      setupTexture(((ExtendedTexture)tex).getMultiTex(), image, width, height, false, false);
   }

   public static void updateTextureMinMagFilter() {
      TextureManager texman = MinecraftClient.getInstance().getTextureManager();
      Texture texObj = texman.getTexture(SpriteAtlasTexture.BLOCK_ATLAS_TEX);
      if (texObj != null) {
         MultiTexID multiTex = ((ExtendedTexture)texObj).getMultiTex();
         GlStateManager.bindTexture(multiTex.base);
         GL11.glTexParameteri(3553, 10241, Shaders.texMinFilValue[Shaders.configTexMinFilB]);
         GL11.glTexParameteri(3553, 10240, Shaders.texMagFilValue[Shaders.configTexMagFilB]);
         GlStateManager.bindTexture(multiTex.norm);
         GL11.glTexParameteri(3553, 10241, Shaders.texMinFilValue[Shaders.configTexMinFilN]);
         GL11.glTexParameteri(3553, 10240, Shaders.texMagFilValue[Shaders.configTexMagFilN]);
         GlStateManager.bindTexture(multiTex.spec);
         GL11.glTexParameteri(3553, 10241, Shaders.texMinFilValue[Shaders.configTexMinFilS]);
         GL11.glTexParameteri(3553, 10240, Shaders.texMagFilValue[Shaders.configTexMagFilS]);
         GlStateManager.bindTexture(0);
      }
   }

   public static int[][] getFrameTexData(int[][] src, int width, int height, int frameIndex) {
      int numLevel = src.length;
      int[][] dst = new int[numLevel][];

      for (int level = 0; level < numLevel; level++) {
         int[] sr1 = src[level];
         if (sr1 != null) {
            int frameSize = (width >> level) * (height >> level);
            int[] ds1 = new int[frameSize * 3];
            dst[level] = ds1;
            int srcSize = sr1.length / 3;
            int srcPos = frameSize * frameIndex;
            int dstPos = 0;
            System.arraycopy(sr1, srcPos, ds1, dstPos, frameSize);
            srcPos += srcSize;
            dstPos += frameSize;
            System.arraycopy(sr1, srcPos, ds1, dstPos, frameSize);
            srcPos += srcSize;
            dstPos += frameSize;
            System.arraycopy(sr1, srcPos, ds1, dstPos, frameSize);
         }
      }

      return dst;
   }

   public static int[][] prepareAF(Sprite tas, int[][] src, int width, int height) {
      boolean skip = true;
      return src;
   }

   public static void fixTransparentColor(Sprite tas, int[] aint) {
   }
}

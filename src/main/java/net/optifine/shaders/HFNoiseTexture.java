package net.optifine.shaders;

import com.mojang.blaze3d.platform.GlStateManager;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;

public class HFNoiseTexture implements ICustomTexture {
   private int texID = GL11.glGenTextures();
   private int textureUnit = 15;

   public HFNoiseTexture(int width, int height) {
      byte[] image = this.genHFNoiseImage(width, height);
      ByteBuffer data = BufferUtils.createByteBuffer(image.length);
      data.put(image);
      ((Buffer)data).flip();
      GlStateManager.bindTexture(this.texID);
      GL11.glTexImage2D(3553, 0, 6407, width, height, 0, 6407, 5121, data);
      GL11.glTexParameteri(3553, 10242, 10497);
      GL11.glTexParameteri(3553, 10243, 10497);
      GL11.glTexParameteri(3553, 10240, 9729);
      GL11.glTexParameteri(3553, 10241, 9729);
      GlStateManager.bindTexture(0);
   }

   public int getID() {
      return this.texID;
   }

   @Override
   public void deleteTexture() {
      GlStateManager.deleteTexture(this.texID);
      this.texID = 0;
   }

   private int random(int seed) {
      seed ^= seed << 13;
      seed ^= seed >> 17;
      return seed ^ seed << 5;
   }

   private byte random(int x, int y, int z) {
      int seed = (this.random(x) + this.random(y * 19)) * this.random(z * 23) - z;
      return (byte)(this.random(seed) % 128);
   }

   private byte[] genHFNoiseImage(int width, int height) {
      byte[] image = new byte[width * height * 3];
      int index = 0;

      for (int y = 0; y < height; y++) {
         for (int x = 0; x < width; x++) {
            for (int z = 1; z < 4; z++) {
               image[index++] = this.random(x, y, z);
            }
         }
      }

      return image;
   }

   @Override
   public int getTextureId() {
      return this.texID;
   }

   @Override
   public int getTextureUnit() {
      return this.textureUnit;
   }

   @Override
   public int getTarget() {
      return 3553;
   }
}

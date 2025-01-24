package net.optifine.shaders;

import net.minecraft.client.texture.Texture;
import net.minecraft.client.texture.TextureUtil;

public class CustomTexture implements ICustomTexture {
   private int textureUnit = -1;
   private String path = null;
   private Texture texture = null;

   public CustomTexture(int textureUnit, String path, Texture texture) {
      this.textureUnit = textureUnit;
      this.path = path;
      this.texture = texture;
   }

   @Override
   public int getTextureUnit() {
      return this.textureUnit;
   }

   public String getPath() {
      return this.path;
   }

   public Texture getTexture() {
      return this.texture;
   }

   @Override
   public int getTextureId() {
      return this.texture.getGlId();
   }

   @Override
   public void deleteTexture() {
      TextureUtil.deleteTexture(this.texture.getGlId());
   }

   @Override
   public int getTarget() {
      return 3553;
   }

   @Override
   public String toString() {
      return "textureUnit: " + this.textureUnit + ", path: " + this.path + ", glTextureId: " + this.getTextureId();
   }
}

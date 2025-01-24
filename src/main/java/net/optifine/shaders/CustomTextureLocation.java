package net.optifine.shaders;

import dev.vexor.photon.tex.ExtendedTexture;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.AbstractTexture;
import net.minecraft.client.texture.ResourceTexture;
import net.minecraft.client.texture.Texture;
import net.minecraft.client.texture.TextureManager;
import net.minecraft.util.Identifier;

public class CustomTextureLocation implements ICustomTexture {
   private int textureUnit = -1;
   private Identifier location;
   private int variant = 0;
   private Texture texture;
   public static final int VARIANT_BASE = 0;
   public static final int VARIANT_NORMAL = 1;
   public static final int VARIANT_SPECULAR = 2;

   public CustomTextureLocation(int textureUnit, Identifier location, int variant) {
      this.textureUnit = textureUnit;
      this.location = location;
      this.variant = variant;
   }

   public Texture getTexture() {
      if (this.texture == null) {
         TextureManager textureManager = MinecraftClient.getInstance().getTextureManager();
         this.texture = textureManager.getTexture(this.location);
         if (this.texture == null) {
            this.texture = new ResourceTexture(this.location);
            textureManager.loadTexture(this.location, this.texture);
            this.texture = textureManager.getTexture(this.location);
         }
      }

      return this.texture;
   }

   @Override
   public int getTextureId() {
      Texture tex = this.getTexture();
      if (this.variant != 0 && tex instanceof AbstractTexture) {
         AbstractTexture at = (AbstractTexture)tex;
         MultiTexID mtid = ((ExtendedTexture) at).getMultiTex();
         if (mtid != null) {
            if (this.variant == 1) {
               return mtid.norm;
            }

            if (this.variant == 2) {
               return mtid.spec;
            }
         }
      }

      return tex.getGlId();
   }

   @Override
   public int getTextureUnit() {
      return this.textureUnit;
   }

   @Override
   public void deleteTexture() {
   }

   @Override
   public int getTarget() {
      return 3553;
   }

   @Override
   public String toString() {
      return "textureUnit: " + this.textureUnit + ", location: " + this.location + ", glTextureId: " + (this.texture != null ? this.texture.getGlId() : "");
   }
}

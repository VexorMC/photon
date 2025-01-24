package net.optifine.shaders;

import dev.vexor.photon.tex.ExtendedTexture;
import net.minecraft.client.texture.AbstractTexture;
import net.minecraft.resource.ResourceManager;

public class DefaultTexture extends AbstractTexture {
   public DefaultTexture() {
      this.load(null);
   }

   public void load(ResourceManager manager) {
      int[] aint = ShadersTex.createAIntImage(1, -1);
      ShadersTex.setupTexture(((ExtendedTexture)this).getMultiTex(), aint, 1, 1, false, false);
   }
}

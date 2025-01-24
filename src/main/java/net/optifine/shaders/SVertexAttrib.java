package net.optifine.shaders;

import net.minecraft.client.render.VertexFormatElement.Format;

public class SVertexAttrib {
   public int index;
   public int count;
   public Format type;
   public int offset;

   public SVertexAttrib(int index, int count, Format type) {
      this.index = index;
      this.count = count;
      this.type = type;
   }
}

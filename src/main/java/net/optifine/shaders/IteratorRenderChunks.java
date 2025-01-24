package net.optifine.shaders;

import java.util.Iterator;
import net.minecraft.client.render.BuiltChunkStorage;
import net.minecraft.client.world.BuiltChunk;
import net.minecraft.util.math.BlockPos;

public class IteratorRenderChunks implements Iterator<BuiltChunk> {
   private BuiltChunkStorage viewFrustum;
   private Iterator3d Iterator3d;
   private BlockPos.Mutable posBlock = new BlockPos.Mutable(0, 0, 0);

   public IteratorRenderChunks(BuiltChunkStorage viewFrustum, BlockPos posStart, BlockPos posEnd, int width, int height) {
      this.viewFrustum = viewFrustum;
      this.Iterator3d = new Iterator3d(posStart, posEnd, width, height);
   }

   @Override
   public boolean hasNext() {
      return this.Iterator3d.hasNext();
   }

   public BuiltChunk next() {
      BlockPos pos = this.Iterator3d.next();
      this.posBlock.setPosition(pos.getX() << 4, pos.getY() << 4, pos.getZ() << 4);
      return this.viewFrustum.getRenderedChunk(this.posBlock);
   }

   @Override
   public void remove() {
      throw new RuntimeException("Not implemented");
   }
}

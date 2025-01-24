package net.optifine.shaders;

import java.util.Iterator;
import java.util.NoSuchElementException;
import net.minecraft.util.math.BlockPos;

public class IteratorAxis implements Iterator<BlockPos> {
   private double yDelta;
   private double zDelta;
   private int xStart;
   private int xEnd;
   private double yStart;
   private double yEnd;
   private double zStart;
   private double zEnd;
   private int xNext;
   private double yNext;
   private double zNext;
   private BlockPos.Mutable pos = new BlockPos.Mutable(0, 0, 0);
   private boolean hasNext = false;

   public IteratorAxis(BlockPos posStart, BlockPos posEnd, double yDelta, double zDelta) {
      this.yDelta = yDelta;
      this.zDelta = zDelta;
      this.xStart = posStart.getX();
      this.xEnd = posEnd.getX();
      this.yStart = (double)posStart.getY();
      this.yEnd = (double)posEnd.getY() - 0.5;
      this.zStart = (double)posStart.getZ();
      this.zEnd = (double)posEnd.getZ() - 0.5;
      this.xNext = this.xStart;
      this.yNext = this.yStart;
      this.zNext = this.zStart;
      this.hasNext = this.xNext < this.xEnd && this.yNext < this.yEnd && this.zNext < this.zEnd;
   }

   @Override
   public boolean hasNext() {
      return this.hasNext;
   }

   public BlockPos next() {
      if (!this.hasNext) {
         throw new NoSuchElementException();
      } else {
         this.pos.setPosition(this.xNext, (int)this.yNext, (int)this.zNext);
         this.nextPos();
         this.hasNext = this.xNext < this.xEnd && this.yNext < this.yEnd && this.zNext < this.zEnd;
         return this.pos;
      }
   }

   private void nextPos() {
      this.zNext++;
      if (!(this.zNext < this.zEnd)) {
         this.zNext = this.zStart;
         this.yNext++;
         if (!(this.yNext < this.yEnd)) {
            this.yNext = this.yStart;
            this.yStart = this.yStart + this.yDelta;
            this.yEnd = this.yEnd + this.yDelta;
            this.yNext = this.yStart;
            this.zStart = this.zStart + this.zDelta;
            this.zEnd = this.zEnd + this.zDelta;
            this.zNext = this.zStart;
            this.xNext++;
            if (this.xNext >= this.xEnd) {
               ;
            }
         }
      }
   }

   @Override
   public void remove() {
      throw new RuntimeException("Not implemented");
   }

   public static void main(String[] args) throws Exception {
      BlockPos posStart = new BlockPos(-2, 10, 20);
      BlockPos posEnd = new BlockPos(2, 12, 22);
      double yDelta = -0.5;
      double zDelta = 0.5;
      IteratorAxis it = new IteratorAxis(posStart, posEnd, yDelta, zDelta);
      System.out.println("Start: " + posStart + ", end: " + posEnd + ", yDelta: " + yDelta + ", zDelta: " + zDelta);

      while (it.hasNext()) {
         BlockPos blockPos = it.next();
         System.out.println("" + blockPos);
      }
   }
}

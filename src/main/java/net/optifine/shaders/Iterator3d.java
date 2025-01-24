package net.optifine.shaders;

import java.util.Iterator;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

public class Iterator3d implements Iterator<BlockPos> {
   private IteratorAxis iteratorAxis;
   private BlockPos.Mutable blockPos = new BlockPos.Mutable(0, 0, 0);
   private int axis = 0;
   private int kX;
   private int kY;
   private int kZ;
   private static final int AXIS_X = 0;
   private static final int AXIS_Y = 1;
   private static final int AXIS_Z = 2;

   public Iterator3d(BlockPos posStart, BlockPos posEnd, int width, int height) {
      boolean revX = posStart.getX() > posEnd.getX();
      boolean revY = posStart.getY() > posEnd.getY();
      boolean revZ = posStart.getZ() > posEnd.getZ();
      posStart = this.reverseCoord(posStart, revX, revY, revZ);
      posEnd = this.reverseCoord(posEnd, revX, revY, revZ);
      this.kX = revX ? -1 : 1;
      this.kY = revY ? -1 : 1;
      this.kZ = revZ ? -1 : 1;
      Vec3d vec = new Vec3d((double)(posEnd.getX() - posStart.getX()), (double)(posEnd.getY() - posStart.getY()), (double)(posEnd.getZ() - posStart.getZ()));
      Vec3d vecN = vec.normalize();
      Vec3d vecX = new Vec3d(1.0, 0.0, 0.0);
      double dotX = vecN.dotProduct(vecX);
      double dotXabs = Math.abs(dotX);
      Vec3d vecY = new Vec3d(0.0, 1.0, 0.0);
      double dotY = vecN.dotProduct(vecY);
      double dotYabs = Math.abs(dotY);
      Vec3d vecZ = new Vec3d(0.0, 0.0, 1.0);
      double dotZ = vecN.dotProduct(vecZ);
      double dotZabs = Math.abs(dotZ);
      if (dotZabs >= dotYabs && dotZabs >= dotXabs) {
         this.axis = 2;
         BlockPos pos1 = new BlockPos(posStart.getZ(), posStart.getY() - width, posStart.getX() - height);
         BlockPos pos2 = new BlockPos(posEnd.getZ(), posStart.getY() + width + 1, posStart.getX() + height + 1);
         int countX = posEnd.getZ() - posStart.getZ();
         double deltaY = (double)(posEnd.getY() - posStart.getY()) / (1.0 * (double)countX);
         double deltaZ = (double)(posEnd.getX() - posStart.getX()) / (1.0 * (double)countX);
         this.iteratorAxis = new IteratorAxis(pos1, pos2, deltaY, deltaZ);
      } else if (dotYabs >= dotXabs && dotYabs >= dotZabs) {
         this.axis = 1;
         BlockPos pos1 = new BlockPos(posStart.getY(), posStart.getX() - width, posStart.getZ() - height);
         BlockPos pos2 = new BlockPos(posEnd.getY(), posStart.getX() + width + 1, posStart.getZ() + height + 1);
         int countX = posEnd.getY() - posStart.getY();
         double deltaY = (double)(posEnd.getX() - posStart.getX()) / (1.0 * (double)countX);
         double deltaZ = (double)(posEnd.getZ() - posStart.getZ()) / (1.0 * (double)countX);
         this.iteratorAxis = new IteratorAxis(pos1, pos2, deltaY, deltaZ);
      } else {
         this.axis = 0;
         BlockPos pos1 = new BlockPos(posStart.getX(), posStart.getY() - width, posStart.getZ() - height);
         BlockPos pos2 = new BlockPos(posEnd.getX(), posStart.getY() + width + 1, posStart.getZ() + height + 1);
         int countX = posEnd.getX() - posStart.getX();
         double deltaY = (double)(posEnd.getY() - posStart.getY()) / (1.0 * (double)countX);
         double deltaZ = (double)(posEnd.getZ() - posStart.getZ()) / (1.0 * (double)countX);
         this.iteratorAxis = new IteratorAxis(pos1, pos2, deltaY, deltaZ);
      }
   }

   private BlockPos reverseCoord(BlockPos pos, boolean revX, boolean revY, boolean revZ) {
      if (revX) {
         pos = new BlockPos(-pos.getX(), pos.getY(), pos.getZ());
      }

      if (revY) {
         pos = new BlockPos(pos.getX(), -pos.getY(), pos.getZ());
      }

      if (revZ) {
         pos = new BlockPos(pos.getX(), pos.getY(), -pos.getZ());
      }

      return pos;
   }

   @Override
   public boolean hasNext() {
      return this.iteratorAxis.hasNext();
   }

   public BlockPos next() {
      BlockPos pos = this.iteratorAxis.next();
      switch (this.axis) {
         case 0:
            this.blockPos.setPosition(pos.getX() * this.kX, pos.getY() * this.kY, pos.getZ() * this.kZ);
            return this.blockPos;
         case 1:
            this.blockPos.setPosition(pos.getY() * this.kX, pos.getX() * this.kY, pos.getZ() * this.kZ);
            return this.blockPos;
         case 2:
            this.blockPos.setPosition(pos.getZ() * this.kX, pos.getY() * this.kY, pos.getX() * this.kZ);
            return this.blockPos;
         default:
            this.blockPos.setPosition(pos.getX() * this.kX, pos.getY() * this.kY, pos.getZ() * this.kZ);
            return this.blockPos;
      }
   }

   @Override
   public void remove() {
      throw new RuntimeException("Not supported");
   }

   public static void main(String[] args) {
      BlockPos posStart = new BlockPos(10, 20, 30);
      BlockPos posEnd = new BlockPos(30, 40, 20);
      Iterator3d it = new Iterator3d(posStart, posEnd, 1, 1);

      while (it.hasNext()) {
         BlockPos blockPos = it.next();
         System.out.println("" + blockPos);
      }
   }
}

package net.optifine.shaders;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import net.minecraft.client.render.BuiltChunkStorage;
import net.minecraft.client.world.BuiltChunk;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.optifine.util.MathUtils;

public class ShadowUtils {
   public static final float PI = MathUtils.roundToFloat(Math.PI);
   public static final float PI2 = MathUtils.roundToFloat((Math.PI * 2D));
   public static final float PId2 = MathUtils.roundToFloat((Math.PI / 2D));
   public static final float deg2Rad = MathUtils.roundToFloat(0.017453292519943295D);

   public static Iterator<BuiltChunk> makeShadowChunkIterator(
      ClientWorld world, double partialTicks, Entity viewEntity, int renderDistanceChunks, BuiltChunkStorage viewFrustum
   ) {
      float shadowRenderDistance = Shaders.getShadowRenderDistance();
      if (!(shadowRenderDistance <= 0.0F) && !(shadowRenderDistance >= (float)((renderDistanceChunks - 1) * 16))) {
         int shadowDistanceChunks = MathHelper.ceil(shadowRenderDistance / 16.0F) + 1;
         float car = world.getSkyAngleRadians((float)partialTicks);
         float sunTiltRad = Shaders.sunPathRotation * deg2Rad;
         float sar = car > PId2 && car < 3.0F * PId2 ? car + PI : car;
         float dx = -MathHelper.sin(sar);
         float dy = MathHelper.cos(sar) * MathHelper.cos(sunTiltRad);
         float dz = -MathHelper.cos(sar) * MathHelper.sin(sunTiltRad);
         BlockPos posEntity = new BlockPos(MathHelper.floor(viewEntity.x) >> 4, MathHelper.floor(viewEntity.y) >> 4, MathHelper.floor(viewEntity.z) >> 4);
         BlockPos posStart = posEntity.add(
            (double)(-dx * (float)shadowDistanceChunks), (double)(-dy * (float)shadowDistanceChunks), (double)(-dz * (float)shadowDistanceChunks)
         );
         BlockPos posEnd = posEntity.add(
            (double)(dx * (float)renderDistanceChunks), (double)(dy * (float)renderDistanceChunks), (double)(dz * (float)renderDistanceChunks)
         );
         return new IteratorRenderChunks(viewFrustum, posStart, posEnd, shadowDistanceChunks, shadowDistanceChunks);
      } else {
         List<BuiltChunk> listChunks = Arrays.asList(viewFrustum.chunks);
         return listChunks.iterator();
      }
   }
}

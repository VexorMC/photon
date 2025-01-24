package net.optifine.shaders.uniform;

import java.util.HashMap;
import java.util.Map;
import net.optifine.util.CounterInt;
import net.optifine.util.SmoothFloat;

public class Smoother {
   private static Map<Integer, SmoothFloat> mapSmoothValues = new HashMap<>();
   private static CounterInt counterIds = new CounterInt(1);

   public static float getSmoothValue(int id, float value, float timeFadeUpSec, float timeFadeDownSec) {
      synchronized (mapSmoothValues) {
         Integer key = id;
         SmoothFloat sf = mapSmoothValues.get(key);
         if (sf == null) {
            sf = new SmoothFloat(value, timeFadeUpSec, timeFadeDownSec);
            mapSmoothValues.put(key, sf);
         }

         return sf.getSmoothValue(value, timeFadeUpSec, timeFadeDownSec);
      }
   }

   public static int getNextId() {
      synchronized (counterIds) {
         return counterIds.nextValue();
      }
   }

   public static void resetValues() {
      synchronized (mapSmoothValues) {
         mapSmoothValues.clear();
      }
   }
}

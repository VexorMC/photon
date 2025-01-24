package net.optifine.shaders;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import dev.vexor.photon.Config;
import dev.vexor.photon.Photon;
import net.minecraft.util.Identifier;
import net.optifine.config.ConnectedParser;
import net.optifine.shaders.config.MacroProcessor;
import net.optifine.util.PropertiesOrdered;
import net.optifine.util.StrUtils;

public class ItemAliases {
   private static int[] itemAliases = null;
   private static boolean updateOnResourcesReloaded;
   private static final int NO_ALIAS = Integer.MIN_VALUE;

   public static int getItemAliasId(int itemId) {
      if (itemAliases == null) {
         return itemId;
      } else if (itemId >= 0 && itemId < itemAliases.length) {
         int aliasId = itemAliases[itemId];
         return aliasId == Integer.MIN_VALUE ? itemId : aliasId;
      } else {
         return itemId;
      }
   }

   public static void resourcesReloaded() {
      if (updateOnResourcesReloaded) {
         updateOnResourcesReloaded = false;
         update(Shaders.getShaderPack());
      }
   }

   public static void update(IShaderPack shaderPack) {
      reset();
      if (shaderPack != null) {
            List<Integer> listItemAliases = new ArrayList<>();
            String path = "/shaders/item.properties";
            InputStream in = shaderPack.getResourceAsStream(path);
            if (in != null) {
               loadItemAliases(in, path, listItemAliases);
            }

            if (listItemAliases.size() > 0) {
               itemAliases = toArray(listItemAliases);
            }
      }
   }

   private static void loadItemAliases(InputStream in, String path, List<Integer> listItemAliases) {
      if (in != null) {
         try {
            in = MacroProcessor.process(in, path);
            Properties props = new PropertiesOrdered();
            props.load(in);
            in.close();
            Photon.LOGGER.debug("[Shaders] Parsing item mappings: " + path);
            ConnectedParser cp = new ConnectedParser("Shaders");

            for (String key : props.keySet().toArray(new String[0])) {
               String val = props.getProperty(key);
               String prefix = "item.";
               if (!key.startsWith(prefix)) {
                  Photon.LOGGER.warn("[Shaders] Invalid item ID: " + key);
               } else {
                  String aliasIdStr = StrUtils.removePrefix(key, prefix);
                  int aliasId = Config.parseInt(aliasIdStr, -1);
                  if (aliasId < 0) {
                     Photon.LOGGER.warn("[Shaders] Invalid item alias ID: " + aliasId);
                  } else {
                     int[] itemIds = cp.parseItems(val);
                     if (itemIds != null && itemIds.length >= 1) {
                        for (int i = 0; i < itemIds.length; i++) {
                           int itemId = itemIds[i];
                           addToList(listItemAliases, itemId, aliasId);
                        }
                     } else {
                        Photon.LOGGER.warn("[Shaders] Invalid item ID mapping: " + key + "=" + val);
                     }
                  }
               }
            }
         } catch (IOException var15) {
            Photon.LOGGER.warn("[Shaders] Error reading: " + path);
         }
      }
   }

   private static void addToList(List<Integer> list, int index, int val) {
      while (list.size() <= index) {
         list.add(Integer.MIN_VALUE);
      }

      list.set(index, val);
   }

   private static int[] toArray(List<Integer> list) {
      int[] arr = new int[list.size()];

      for (int i = 0; i < arr.length; i++) {
         arr[i] = list.get(i);
      }

      return arr;
   }

   public static void reset() {
      itemAliases = null;
   }
}

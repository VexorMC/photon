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

public class EntityAliases {
   private static int[] entityAliases = null;
   private static boolean updateOnResourcesReloaded;

   public static int getEntityAliasId(int entityId) {
      if (entityAliases == null) {
         return -1;
      } else {
         return entityId >= 0 && entityId < entityAliases.length ? entityAliases[entityId] : -1;
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
            List<Integer> listEntityAliases = new ArrayList<>();
            String path = "/shaders/entity.properties";
            InputStream in = shaderPack.getResourceAsStream(path);
            if (in != null) {
               loadEntityAliases(in, path, listEntityAliases);
            }

            if (listEntityAliases.size() > 0) {
               entityAliases = toArray(listEntityAliases);
            }
      }
   }

   private static void loadEntityAliases(InputStream in, String path, List<Integer> listEntityAliases) {
      if (in != null) {
         try {
            in = MacroProcessor.process(in, path);
            Properties props = new PropertiesOrdered();
            props.load(in);
            in.close();
            Photon.LOGGER.debug("[Shaders] Parsing entity mappings: " + path);
            ConnectedParser cp = new ConnectedParser("Shaders");

            for (String key : props.keySet().toArray(new String[0])) {
               String val = props.getProperty(key);
               String prefix = "entity.";
               if (!key.startsWith(prefix)) {
                  Photon.LOGGER.warn("[Shaders] Invalid entity ID: " + key);
               } else {
                  String aliasIdStr = StrUtils.removePrefix(key, prefix);
                  int aliasId = Config.parseInt(aliasIdStr, -1);
                  if (aliasId < 0) {
                     Photon.LOGGER.warn("[Shaders] Invalid entity alias ID: " + aliasId);
                  } else {
                     int[] entityIds = cp.parseEntities(val);
                     if (entityIds != null && entityIds.length >= 1) {
                        for (int i = 0; i < entityIds.length; i++) {
                           int entityId = entityIds[i];
                           addToList(listEntityAliases, entityId, aliasId);
                        }
                     } else {
                        Photon.LOGGER.warn("[Shaders] Invalid entity ID mapping: " + key + "=" + val);
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
         list.add(-1);
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
      entityAliases = null;
   }
}

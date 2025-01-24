package net.optifine.shaders;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import dev.vexor.photon.Config;
import dev.vexor.photon.Photon;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.Identifier;
import net.optifine.config.ConnectedParser;
import net.optifine.config.MatchBlock;
import net.optifine.shaders.config.MacroProcessor;
import net.optifine.util.PropertiesOrdered;
import net.optifine.util.StrUtils;

public class BlockAliases {
   private static BlockAlias[][] blockAliases = (BlockAlias[][])null;
   private static PropertiesOrdered blockLayerPropertes = null;
   private static boolean updateOnResourcesReloaded;

   public static int getBlockAliasId(int blockId, int metadata) {
      if (blockAliases == null) {
         return blockId;
      } else if (blockId >= 0 && blockId < blockAliases.length) {
         BlockAlias[] aliases = blockAliases[blockId];
         if (aliases == null) {
            return blockId;
         } else {
            for (int i = 0; i < aliases.length; i++) {
               BlockAlias ba = aliases[i];
               if (ba.matches(blockId, metadata)) {
                  return ba.getBlockAliasId();
               }
            }

            return blockId;
         }
      } else {
         return blockId;
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
            List<List<BlockAlias>> listBlockAliases = new ArrayList<>();
            String path = "/shaders/block.properties";
            InputStream in = shaderPack.getResourceAsStream(path);
            if (in != null) {
               loadBlockAliases(in, path, listBlockAliases);
            }

            if (listBlockAliases.size() > 0) {
               blockAliases = toArrays(listBlockAliases);
            }
      }
   }

   private static void loadBlockAliases(InputStream in, String path, List<List<BlockAlias>> listBlockAliases) {
      if (in != null) {
         try {
            in = MacroProcessor.process(in, path);
            Properties props = new PropertiesOrdered();
            props.load(in);
            in.close();
            Photon.LOGGER.debug("[Shaders] Parsing block mappings: " + path);
            ConnectedParser cp = new ConnectedParser("Shaders");

            for (String key : props.keySet().toArray(new String[0])) {
               String val = props.getProperty(key);
               if (key.startsWith("layer.")) {
                  if (blockLayerPropertes == null) {
                     blockLayerPropertes = new PropertiesOrdered();
                  }

                  blockLayerPropertes.put(key, val);
               } else {
                  String prefix = "block.";
                  if (!key.startsWith(prefix)) {
                     Photon.LOGGER.warn("[Shaders] Invalid block ID: " + key);
                  } else {
                     String blockIdStr = StrUtils.removePrefix(key, prefix);
                     int blockId = Config.parseInt(blockIdStr, -1);
                     if (blockId < 0) {
                        Photon.LOGGER.warn("[Shaders] Invalid block ID: " + key);
                     } else {
                        MatchBlock[] matchBlocks = cp.parseMatchBlocks(val);
                        if (matchBlocks != null && matchBlocks.length >= 1) {
                           BlockAlias ba = new BlockAlias(blockId, matchBlocks);
                           addToList(listBlockAliases, ba);
                        } else {
                           Photon.LOGGER.warn("[Shaders] Invalid block ID mapping: " + key + "=" + val);
                        }
                     }
                  }
               }
            }
         } catch (IOException var14) {
            Photon.LOGGER.warn("[Shaders] Error reading: " + path);
         }
      }
   }

   private static void addToList(List<List<BlockAlias>> blocksAliases, BlockAlias ba) {
      int[] blockIds = ba.getMatchBlockIds();

      for (int i = 0; i < blockIds.length; i++) {
         int blockId = blockIds[i];

         while (blockId >= blocksAliases.size()) {
            blocksAliases.add(null);
         }

         List<BlockAlias> blockAliases = blocksAliases.get(blockId);
         if (blockAliases == null) {
            blockAliases = new ArrayList<>();
            blocksAliases.set(blockId, blockAliases);
         }

         BlockAlias baBlock = new BlockAlias(ba.getBlockAliasId(), ba.getMatchBlocks(blockId));
         blockAliases.add(baBlock);
      }
   }

   private static BlockAlias[][] toArrays(List<List<BlockAlias>> listBlocksAliases) {
      BlockAlias[][] bas = new BlockAlias[listBlocksAliases.size()][];

      for (int i = 0; i < bas.length; i++) {
         List<BlockAlias> listBlockAliases = listBlocksAliases.get(i);
         if (listBlockAliases != null) {
            bas[i] = listBlockAliases.toArray(new BlockAlias[listBlockAliases.size()]);
         }
      }

      return bas;
   }

   public static PropertiesOrdered getBlockLayerPropertes() {
      return blockLayerPropertes;
   }

   public static void reset() {
      blockAliases = (BlockAlias[][])null;
      blockLayerPropertes = null;
   }
}

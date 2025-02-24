package net.optifine.shaders;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import net.optifine.util.StrUtils;

public class ShaderPackFolder implements IShaderPack {
   protected File packFile;

   public ShaderPackFolder(String name, File file) {
      this.packFile = file;
   }

   @Override
   public void close() {
   }

   @Override
   public InputStream getResourceAsStream(String resName) {
      try {
         String name = StrUtils.removePrefixSuffix(resName, "/", "/");
         File resFile = new File(this.packFile, name);
         return !resFile.exists() ? null : new BufferedInputStream(new FileInputStream(resFile));
      } catch (Exception var4) {
         return null;
      }
   }

   @Override
   public boolean hasDirectory(String name) {
      File resFile = new File(this.packFile, name.substring(1));
      return !resFile.exists() ? false : resFile.isDirectory();
   }

   @Override
   public String getName() {
      return this.packFile.getName();
   }
}

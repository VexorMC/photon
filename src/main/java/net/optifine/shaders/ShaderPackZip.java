package net.optifine.shaders;

import com.google.common.base.Joiner;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Enumeration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import dev.vexor.photon.Config;
import net.optifine.util.StrUtils;

public class ShaderPackZip implements IShaderPack {
   protected File packFile;
   protected ZipFile packZipFile;
   protected String baseFolder;

   public ShaderPackZip(String name, File file) {
      this.packFile = file;
      this.packZipFile = null;
      this.baseFolder = "";
   }

   @Override
   public void close() {
      if (this.packZipFile != null) {
         try {
            this.packZipFile.close();
         } catch (Exception var2) {
         }

         this.packZipFile = null;
      }
   }

   @Override
   public InputStream getResourceAsStream(String resName) {
      try {
         if (this.packZipFile == null) {
            this.packZipFile = new ZipFile(this.packFile);
            this.baseFolder = this.detectBaseFolder(this.packZipFile);
         }

         String name = StrUtils.removePrefix(resName, "/");
         if (name.contains("..")) {
            name = this.resolveRelative(name);
         }

         ZipEntry entry = this.packZipFile.getEntry(this.baseFolder + name);
         return entry == null ? null : this.packZipFile.getInputStream(entry);
      } catch (Exception var4) {
         return null;
      }
   }

   private String resolveRelative(String name) {
      Deque<String> stack = new ArrayDeque<>();
      String[] parts = Config.tokenize(name, "/");

      for (int i = 0; i < parts.length; i++) {
         String part = parts[i];
         if (part.equals("..")) {
            if (stack.isEmpty()) {
               return "";
            }

            stack.removeLast();
         } else {
            stack.add(part);
         }
      }

      return Joiner.on('/').join(stack);
   }

   private String detectBaseFolder(ZipFile zip) {
      ZipEntry entryShaders = zip.getEntry("shaders/");
      if (entryShaders != null && entryShaders.isDirectory()) {
         return "";
      } else {
         Pattern patternFolderShaders = Pattern.compile("([^/]+/)shaders/");
         Enumeration<? extends ZipEntry> entries = zip.entries();

         while (entries.hasMoreElements()) {
            ZipEntry entry = entries.nextElement();
            String name = entry.getName();
            Matcher matcher = patternFolderShaders.matcher(name);
            if (matcher.matches()) {
               String base = matcher.group(1);
               if (base != null) {
                  if (base.equals("shaders/")) {
                     return "";
                  }

                  return base;
               }
            }
         }

         return "";
      }
   }

   @Override
   public boolean hasDirectory(String resName) {
      try {
         if (this.packZipFile == null) {
            this.packZipFile = new ZipFile(this.packFile);
            this.baseFolder = this.detectBaseFolder(this.packZipFile);
         }

         String name = StrUtils.removePrefix(resName, "/");
         ZipEntry entry = this.packZipFile.getEntry(this.baseFolder + name);
         return entry != null;
      } catch (IOException var4) {
         return false;
      }
   }

   @Override
   public String getName() {
      return this.packFile.getName();
   }
}

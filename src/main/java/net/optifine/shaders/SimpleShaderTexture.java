package net.optifine.shaders;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import net.minecraft.client.resource.AnimationMetadata;
import net.minecraft.client.resource.AnimationMetadataSerializer;
import net.minecraft.client.resource.FontMetadata;
import net.minecraft.client.resource.FontMetadataSerializer;
import net.minecraft.client.resource.LanguageMetadataSerializer;
import net.minecraft.client.resource.PackFormatMetadataSerializer;
import net.minecraft.client.resource.ResourcePackMetadata;
import net.minecraft.client.resource.TextureMetadataSerializer;
import net.minecraft.client.resource.metadata.LanguageResourceMetadata;
import net.minecraft.client.resource.metadata.TextureResourceMetadata;
import net.minecraft.client.texture.AbstractTexture;
import net.minecraft.client.texture.TextureUtil;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.MetadataSerializer;
import org.apache.commons.io.IOUtils;

public class SimpleShaderTexture extends AbstractTexture {
   private String texturePath;
   private static final MetadataSerializer METADATA_SERIALIZER = makeMetadataSerializer();

   public SimpleShaderTexture(String texturePath) {
      this.texturePath = texturePath;
   }

   public void load(ResourceManager manager) throws IOException {
      this.clearGlId();
      InputStream inputStream = Shaders.getShaderPackResourceStream(this.texturePath);
      if (inputStream == null) {
         throw new FileNotFoundException("Shader texture not found: " + this.texturePath);
      } else {
         try {
            BufferedImage bufferedimage = TextureUtil.create(inputStream);
            TextureResourceMetadata tms = loadTextureMetadataSection(this.texturePath, new TextureResourceMetadata(false, false, new ArrayList()));
            TextureUtil.method_5860(this.getGlId(), bufferedimage, tms.method_5980(), tms.method_5981());
         } finally {
            IOUtils.closeQuietly(inputStream);
         }
      }
   }

   public static TextureResourceMetadata loadTextureMetadataSection(String texturePath, TextureResourceMetadata def) {
      String pathMeta = texturePath + ".mcmeta";
      String sectionName = "texture";
      InputStream inMeta = Shaders.getShaderPackResourceStream(pathMeta);
      if (inMeta != null) {
         MetadataSerializer ms = METADATA_SERIALIZER;
         BufferedReader brMeta = new BufferedReader(new InputStreamReader(inMeta));

         TextureResourceMetadata var9;
         try {
            JsonObject jsonMeta = new JsonParser().parse(brMeta).getAsJsonObject();
            TextureResourceMetadata meta = (TextureResourceMetadata)ms.fromJson(sectionName, jsonMeta);
            if (meta == null) {
               return def;
            }

            var9 = meta;
         } catch (RuntimeException var13) {
            SMCLog.warning("Error reading metadata: " + pathMeta);
            SMCLog.warning("" + var13.getClass().getName() + ": " + var13.getMessage());
            return def;
         } finally {
            IOUtils.closeQuietly(brMeta);
            IOUtils.closeQuietly(inMeta);
         }

         return var9;
      } else {
         return def;
      }
   }

   private static MetadataSerializer makeMetadataSerializer() {
      MetadataSerializer ms = new MetadataSerializer();
      ms.register(new TextureMetadataSerializer(), TextureResourceMetadata.class);
      ms.register(new FontMetadataSerializer(), FontMetadata.class);
      ms.register(new AnimationMetadataSerializer(), AnimationMetadata.class);
      ms.register(new PackFormatMetadataSerializer(), ResourcePackMetadata.class);
      ms.register(new LanguageMetadataSerializer(), LanguageResourceMetadata.class);
      return ms;
   }
}

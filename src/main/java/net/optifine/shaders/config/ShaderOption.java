package net.optifine.shaders.config;

import java.util.Arrays;
import java.util.List;

import dev.vexor.photon.Config;
import net.optifine.shaders.Shaders;
import net.optifine.util.NumUtils;
import net.optifine.util.StrUtils;

public abstract class ShaderOption {
   private String name = null;
   private String description = null;
   private String value = null;
   private String[] values = null;
   private String valueDefault = null;
   private String[] paths = null;
   private boolean enabled = true;
   private boolean visible = true;
   public static final String COLOR_GREEN = "§a";
   public static final String COLOR_RED = "§c";
   public static final String COLOR_BLUE = "§9";

   public ShaderOption(String name, String description, String value, String[] values, String valueDefault, String path) {
      this.name = name;
      this.description = description;
      this.value = value;
      this.values = values;
      this.valueDefault = valueDefault;
      if (path != null) {
         this.paths = new String[]{path};
      }
   }

   public String getName() {
      return this.name;
   }

   public String getDescription() {
      return this.description;
   }

   public String getDescriptionText() {
      String desc = Config.normalize(this.description);
      desc = StrUtils.removePrefix(desc, "//");
      return Shaders.translate("option." + this.getName() + ".comment", desc);
   }

   public void setDescription(String description) {
      this.description = description;
   }

   public String getValue() {
      return this.value;
   }

   public boolean setValue(String value) {
      int index = getIndex(value, this.values);
      if (index < 0) {
         return false;
      } else {
         this.value = value;
         return true;
      }
   }

   public String getValueDefault() {
      return this.valueDefault;
   }

   public void resetValue() {
      this.value = this.valueDefault;
   }

   public void nextValue() {
      int index = getIndex(this.value, this.values);
      if (index >= 0) {
         index = (index + 1) % this.values.length;
         this.value = this.values[index];
      }
   }

   public void prevValue() {
      int index = getIndex(this.value, this.values);
      if (index >= 0) {
         index = (index - 1 + this.values.length) % this.values.length;
         this.value = this.values[index];
      }
   }

   private static int getIndex(String str, String[] strs) {
      for (int i = 0; i < strs.length; i++) {
         String s = strs[i];
         if (s.equals(str)) {
            return i;
         }
      }

      return -1;
   }

   public String[] getPaths() {
      return this.paths;
   }

   public void addPaths(String[] newPaths) {
      List<String> pathList = Arrays.asList(this.paths);

      for (int i = 0; i < newPaths.length; i++) {
         String newPath = newPaths[i];
         if (!pathList.contains(newPath)) {
            this.paths = (String[])Config.addObjectToArray(this.paths, newPath);
         }
      }
   }

   public boolean isEnabled() {
      return this.enabled;
   }

   public void setEnabled(boolean enabled) {
      this.enabled = enabled;
   }

   public boolean isChanged() {
      return !Config.equals(this.value, this.valueDefault);
   }

   public boolean isVisible() {
      return this.visible;
   }

   public void setVisible(boolean visible) {
      this.visible = visible;
   }

   public boolean isValidValue(String val) {
      return getIndex(val, this.values) >= 0;
   }

   public String getNameText() {
      return Shaders.translate("option." + this.name, this.name);
   }

   public String getValueText(String val) {
      return Shaders.translate("value." + this.name + "." + val, val);
   }

   public String getValueColor(String val) {
      return "";
   }

   public boolean matchesLine(String line) {
      return false;
   }

   public boolean checkUsed() {
      return false;
   }

   public boolean isUsedInLine(String line) {
      return false;
   }

   public String getSourceLine() {
      return null;
   }

   public String[] getValues() {
      return (String[])this.values.clone();
   }

   public float getIndexNormalized() {
      if (this.values.length <= 1) {
         return 0.0F;
      } else {
         int index = getIndex(this.value, this.values);
         return index < 0 ? 0.0F : 1.0F * (float)index / ((float)this.values.length - 1.0F);
      }
   }

   public void setIndexNormalized(float f) {
      if (this.values.length > 1) {
         f = NumUtils.limit(f, 0.0F, 1.0F);
         int index = Math.round(f * (float)(this.values.length - 1));
         this.value = this.values[index];
      }
   }

   @Override
   public String toString() {
      return "" + this.name + ", value: " + this.value + ", valueDefault: " + this.valueDefault + ", paths: " + Config.arrayToString((Object[])this.paths);
   }
}

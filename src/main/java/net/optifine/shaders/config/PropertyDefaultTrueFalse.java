package net.optifine.shaders.config;

import net.minecraft.client.resource.language.I18n;

public class PropertyDefaultTrueFalse extends Property {
   public static final String[] PROPERTY_VALUES = new String[]{"default", "true", "false"};
   public static final String[] USER_VALUES = new String[]{"Default", "ON", "OFF"};

   public PropertyDefaultTrueFalse(String propertyName, String userName, int defaultValue) {
      super(propertyName, PROPERTY_VALUES, userName, USER_VALUES, defaultValue);
   }

   @Override
   public String getUserValue() {
      if (this.isDefault()) {
         return I18n.translate("generator.default");
      } else if (this.isTrue()) {
         return "On";
      } else {
         return this.isFalse() ? "Off" : super.getUserValue();
      }
   }

   public boolean isDefault() {
      return this.getValue() == 0;
   }

   public boolean isTrue() {
      return this.getValue() == 1;
   }

   public boolean isFalse() {
      return this.getValue() == 2;
   }
}

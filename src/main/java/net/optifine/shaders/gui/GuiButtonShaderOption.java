package net.optifine.shaders.gui;

import net.minecraft.client.gui.widget.ButtonWidget;
import net.optifine.shaders.config.ShaderOption;

public class GuiButtonShaderOption extends ButtonWidget {
   private ShaderOption shaderOption = null;

   public GuiButtonShaderOption(int buttonId, int x, int y, int widthIn, int heightIn, ShaderOption shaderOption, String text) {
      super(buttonId, x, y, widthIn, heightIn, text);
      this.shaderOption = shaderOption;
   }

   public ShaderOption getShaderOption() {
      return this.shaderOption;
   }

   public void valueChanged() {
   }

   public boolean isSwitchable() {
      return true;
   }
}

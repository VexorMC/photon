package net.optifine.shaders.gui;

import com.mojang.blaze3d.platform.GlStateManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.util.math.MathHelper;
import net.optifine.shaders.config.ShaderOption;

public class GuiSliderShaderOption extends GuiButtonShaderOption {
   private float sliderValue;
   public boolean dragging;
   private ShaderOption shaderOption = null;

   public GuiSliderShaderOption(int buttonId, int x, int y, int w, int h, ShaderOption shaderOption, String text) {
      super(buttonId, x, y, w, h, shaderOption, text);
      this.sliderValue = 1.0F;
      this.shaderOption = shaderOption;
      this.sliderValue = shaderOption.getIndexNormalized();
      this.message = GuiShaderOptions.getButtonText(shaderOption, this.width);
   }

   protected int getYImage(boolean isHovered) {
      return 0;
   }

   protected void mouseDragged(MinecraftClient client, int mouseX, int mouseY) {
      if (this.visible) {
         if (this.dragging && !Screen.hasShiftDown()) {
            this.sliderValue = (float)(mouseX - (this.x + 4)) / (float)(this.width - 8);
            this.sliderValue = MathHelper.clamp(this.sliderValue, 0.0F, 1.0F);
            this.shaderOption.setIndexNormalized(this.sliderValue);
            this.sliderValue = this.shaderOption.getIndexNormalized();
            this.message = GuiShaderOptions.getButtonText(this.shaderOption, this.width);
         }

         client.getTextureManager().bindTexture(WIDGETS_LOCATION);
         GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
         this.drawTexture(this.x + (int)(this.sliderValue * (float)(this.width - 8)), this.y, 0, 66, 4, 20);
         this.drawTexture(this.x + (int)(this.sliderValue * (float)(this.width - 8)) + 4, this.y, 196, 66, 4, 20);
      }
   }

   public boolean isMouseOver(MinecraftClient client, int mouseX, int mouseY) {
      if (super.isMouseOver(client, mouseX, mouseY)) {
         this.sliderValue = (float)(mouseX - (this.x + 4)) / (float)(this.width - 8);
         this.sliderValue = MathHelper.clamp(this.sliderValue, 0.0F, 1.0F);
         this.shaderOption.setIndexNormalized(this.sliderValue);
         this.message = GuiShaderOptions.getButtonText(this.shaderOption, this.width);
         this.dragging = true;
         return true;
      } else {
         return false;
      }
   }

   public void mouseReleased(int mouseX, int mouseY) {
      this.dragging = false;
   }

   @Override
   public void valueChanged() {
      this.sliderValue = this.shaderOption.getIndexNormalized();
   }

   @Override
   public boolean isSwitchable() {
      return false;
   }
}

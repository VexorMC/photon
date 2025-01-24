package net.optifine.shaders.gui;

import com.mojang.blaze3d.platform.GlStateManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.util.Identifier;

public class GuiButtonDownloadShaders extends ButtonWidget {
   public GuiButtonDownloadShaders(int buttonID, int xPos, int yPos) {
      super(buttonID, xPos, yPos, 22, 20, "");
   }

   public void render(MinecraftClient client, int mouseX, int mouseY) {
      if (this.visible) {
         super.render(client, mouseX, mouseY);
         Identifier locTexture = new Identifier("optifine/textures/icons.png");
         client.getTextureManager().bindTexture(locTexture);
         GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
         this.drawTexture(this.x + 3, this.y + 2, 0, 0, 16, 16);
      }
   }
}

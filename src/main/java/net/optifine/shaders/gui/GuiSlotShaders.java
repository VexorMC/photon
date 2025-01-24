package net.optifine.shaders.gui;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Properties;

import dev.vexor.photon.Config;
import net.minecraft.client.gui.screen.ConfirmScreen;
import net.minecraft.client.gui.widget.IdentifiableBooleanConsumer;
import net.minecraft.client.gui.widget.ListWidget;
import net.minecraft.client.resource.language.I18n;
import net.optifine.shaders.IShaderPack;
import net.optifine.shaders.Shaders;
import net.optifine.util.ResUtils;

class GuiSlotShaders extends ListWidget {
   private ArrayList shaderslist;
   private int selectedIndex;
   private long lastClickedCached = 0L;
   final GuiShaders shadersGui;

   public GuiSlotShaders(GuiShaders par1GuiShaders, int width, int height, int top, int bottom, int slotHeight) {
      super(par1GuiShaders.getMc(), width, height, top, bottom, slotHeight);
      this.shadersGui = par1GuiShaders;
      this.updateList();
      this.scrollAmount = 0.0F;
      int posYSelected = this.selectedIndex * slotHeight;
      int wMid = (bottom - top) / 2;
      if (posYSelected > wMid) {
         this.scroll(posYSelected - wMid);
      }
   }

   public int getRowWidth() {
      return this.width - 20;
   }

   public void updateList() {
      this.shaderslist = Shaders.listOfShaders();
      this.selectedIndex = 0;
      int i = 0;

      for (int n = this.shaderslist.size(); i < n; i++) {
         if (((String)this.shaderslist.get(i)).equals(Shaders.currentShaderName)) {
            this.selectedIndex = i;
            break;
         }
      }
   }

   protected int getEntryCount() {
      return this.shaderslist.size();
   }

   protected void selectEntry(int index, boolean doubleClick, int lastMouseX, int lastMouseY) {
      if (index != this.selectedIndex || this.time != this.lastClickedCached) {
         String name = (String)this.shaderslist.get(index);
         IShaderPack sp = Shaders.getShaderPack(name);
         if (this.checkCompatible(sp, index)) {
            this.selectIndex(index);
         }
      }
   }

   private void selectIndex(int index) {
      this.selectedIndex = index;
      this.lastClickedCached = this.time;
      Shaders.setShaderPack((String)this.shaderslist.get(index));
      Shaders.uninit();
      this.shadersGui.updateButtons();
   }

   private boolean checkCompatible(IShaderPack sp, int index) {
      if (sp == null) {
         return true;
      } else {
         InputStream in = sp.getResourceAsStream("/shaders/shaders.properties");
         Properties props = ResUtils.readProperties(in, "Shaders");
         if (props == null) {
            return true;
         } else {
            String keyVer = "version.1.8.9";
            String relMin = props.getProperty(keyVer);
            if (relMin == null) {
               return true;
            } else {
               relMin = relMin.trim();
               String rel = "M6_pre2";
               int res = Config.compareRelease(rel, relMin);
               if (res >= 0) {
                  return true;
               } else {
                  String verMin = ("HD_U_" + relMin).replace('_', ' ');
                  String msg1 = I18n.translate("of.message.shaders.nv1", new Object[]{verMin});
                  String msg2 = I18n.translate("of.message.shaders.nv2", new Object[0]);
                  final int theIndex = index;
                  IdentifiableBooleanConsumer callback = new IdentifiableBooleanConsumer() {
                     public void confirmResult(boolean confirmed, int id) {
                        if (confirmed) {
                           GuiSlotShaders.this.selectIndex(theIndex);
                        }

                        GuiSlotShaders.this.client.setScreen(GuiSlotShaders.this.shadersGui);
                     }
                  };
                  ConfirmScreen guiYesNo = new ConfirmScreen(callback, msg1, msg2, 0);
                  this.client.setScreen(guiYesNo);
                  return false;
               }
            }
         }
      }
   }

   protected boolean isEntrySelected(int index) {
      return index == this.selectedIndex;
   }

   protected int getScrollbarPosition() {
      return this.width - 6;
   }

   protected int getMaxPosition() {
      return this.getEntryCount() * 18;
   }

   protected void renderBackground() {
   }

   protected void renderEntry(int index, int x, int y, int rowHeight, int mouseX, int mouseY) {
      String label = (String)this.shaderslist.get(index);
      if (label.equals("OFF")) {
         label = I18n.translate("of.options.shaders.packNone");
      } else if (label.equals("(internal)")) {
         label = I18n.translate("of.options.shaders.packDefault");
      }

      this.shadersGui.drawCenteredString(label, this.width / 2, y + 1, 14737632);
   }

   public int getSelectedIndex() {
      return this.selectedIndex;
   }
}

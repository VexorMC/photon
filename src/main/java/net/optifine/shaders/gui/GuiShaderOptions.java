package net.optifine.shaders.gui;

import dev.vexor.photon.Config;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.util.math.MathHelper;
import net.optifine.gui.GuiScreenOF;
import net.optifine.gui.TooltipManager;
import net.optifine.gui.TooltipProviderShaderOptions;
import net.optifine.shaders.Shaders;
import net.optifine.shaders.config.ShaderOption;
import net.optifine.shaders.config.ShaderOptionProfile;
import net.optifine.shaders.config.ShaderOptionScreen;

public class GuiShaderOptions extends GuiScreenOF {
   private Screen prevScreen;
   protected String title;
   private GameOptions settings;
   private TooltipManager tooltipManager = new TooltipManager(this, new TooltipProviderShaderOptions());
   private String screenName = null;
   private String screenText = null;
   private boolean changed = false;
   public static final String OPTION_PROFILE = "<profile>";
   public static final String OPTION_EMPTY = "<empty>";
   public static final String OPTION_REST = "*";

   public GuiShaderOptions(Screen guiscreen, GameOptions gamesettings) {
      this.title = "Shader Options";
      this.prevScreen = guiscreen;
      this.settings = gamesettings;
   }

   public GuiShaderOptions(Screen guiscreen, GameOptions gamesettings, String screenName) {
      this(guiscreen, gamesettings);
      this.screenName = screenName;
      if (screenName != null) {
         this.screenText = Shaders.translate("screen." + screenName, screenName);
      }
   }

   public void init() {
      this.title = I18n.translate("of.options.shaderOptionsTitle", new Object[0]);
      int baseId = 100;
      int baseX = 0;
      int baseY = 30;
      int stepY = 20;
      int btnWidth = 120;
      int btnHeight = 20;
      int columns = Shaders.getShaderPackColumns(this.screenName, 2);
      ShaderOption[] ops = Shaders.getShaderPackOptions(this.screenName);
      if (ops != null) {
         int colsMin = MathHelper.ceil((double)ops.length / 9.0);
         if (columns < colsMin) {
            columns = colsMin;
         }

         for (int i = 0; i < ops.length; i++) {
            ShaderOption so = ops[i];
            if (so != null && so.isVisible()) {
               int col = i % columns;
               int row = i / columns;
               int colWidth = Math.min(this.width / columns, 200);
               baseX = (this.width - colWidth * columns) / 2;
               int x = col * colWidth + 5 + baseX;
               int y = baseY + row * stepY;
               int w = colWidth - 10;
               String text = getButtonText(so, w);
               GuiButtonShaderOption btn;
               if (Shaders.isShaderPackOptionSlider(so.getName())) {
                  btn = new GuiSliderShaderOption(baseId + i, x, y, w, btnHeight, so, text);
               } else {
                  btn = new GuiButtonShaderOption(baseId + i, x, y, w, btnHeight, so, text);
               }

               btn.active = so.isEnabled();
               this.buttons.add(btn);
            }
         }
      }

      this.buttons
         .add(
            new ButtonWidget(
               201, this.width / 2 - btnWidth - 20, this.height / 6 + 168 + 11, btnWidth, btnHeight, I18n.translate("controls.reset", new Object[0])
            )
         );
      this.buttons.add(new ButtonWidget(200, this.width / 2 + 20, this.height / 6 + 168 + 11, btnWidth, btnHeight, I18n.translate("gui.done", new Object[0])));
   }

   public static String getButtonText(ShaderOption so, int btnWidth) {
      String labelName = so.getNameText();
      if (so instanceof ShaderOptionScreen) {
         ShaderOptionScreen soScr = (ShaderOptionScreen)so;
         return labelName + "...";
      } else {
         TextRenderer fr = Config.getMinecraft().textRenderer;
         int lenSuffix = fr.getStringWidth(": " + "Off") + 5;

         while (fr.getStringWidth(labelName) + lenSuffix >= btnWidth && labelName.length() > 0) {
            labelName = labelName.substring(0, labelName.length() - 1);
         }

         String col = so.isChanged() ? so.getValueColor(so.getValue()) : "";
         String labelValue = so.getValueText(so.getValue());
         return labelName + ": " + col + labelValue;
      }
   }

   protected void buttonClicked(ButtonWidget button) {
      if (button.active) {
         if (button.id < 200 && button instanceof GuiButtonShaderOption) {
            GuiButtonShaderOption btnSo = (GuiButtonShaderOption)button;
            ShaderOption so = btnSo.getShaderOption();
            if (so instanceof ShaderOptionScreen) {
               String screenName = so.getName();
               GuiShaderOptions scr = new GuiShaderOptions(this, this.settings, screenName);
               this.client.setScreen(scr);
               return;
            }

            if (hasShiftDown()) {
               so.resetValue();
            } else if (btnSo.isSwitchable()) {
               so.nextValue();
            }

            this.updateAllButtons();
            this.changed = true;
         }

         if (button.id == 201) {
            ShaderOption[] opts = Shaders.getChangedOptions(Shaders.getShaderPackOptions());

            for (int i = 0; i < opts.length; i++) {
               ShaderOption opt = opts[i];
               opt.resetValue();
               this.changed = true;
            }

            this.updateAllButtons();
         }

         if (button.id == 200) {
            if (this.changed) {
               Shaders.saveShaderPackOptions();
               this.changed = false;
               Shaders.uninit();
            }

            this.client.setScreen(this.prevScreen);
         }
      }
   }

   @Override
   protected void actionPerformedRightClick(ButtonWidget btn) {
      if (btn instanceof GuiButtonShaderOption) {
         GuiButtonShaderOption btnSo = (GuiButtonShaderOption)btn;
         ShaderOption so = btnSo.getShaderOption();
         if (hasShiftDown()) {
            so.resetValue();
         } else if (btnSo.isSwitchable()) {
            so.prevValue();
         }

         this.updateAllButtons();
         this.changed = true;
      }
   }

   public void removed() {
      super.removed();
      if (this.changed) {
         Shaders.saveShaderPackOptions();
         this.changed = false;
         Shaders.uninit();
      }
   }

   private void updateAllButtons() {
      for (ButtonWidget btn : this.buttons) {
         if (btn instanceof GuiButtonShaderOption) {
            GuiButtonShaderOption gbso = (GuiButtonShaderOption)btn;
            ShaderOption opt = gbso.getShaderOption();
            if (opt instanceof ShaderOptionProfile) {
               ShaderOptionProfile optProf = (ShaderOptionProfile)opt;
               optProf.updateProfile();
            }

            gbso.message = getButtonText(opt, gbso.getWidth());
            gbso.valueChanged();
         }
      }
   }

   public void render(int mouseX, int mouseY, float tickDelta) {
      this.renderBackground();
      if (this.screenText != null) {
         this.drawCenteredString(this.textRenderer, this.screenText, this.width / 2, 15, 16777215);
      } else {
         this.drawCenteredString(this.textRenderer, this.title, this.width / 2, 15, 16777215);
      }

      super.render(mouseX, mouseY, tickDelta);
      this.tooltipManager.drawTooltips(mouseX, mouseY, this.buttons);
   }
}

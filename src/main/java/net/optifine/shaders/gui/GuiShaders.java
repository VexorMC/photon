package net.optifine.shaders.gui;

import java.io.File;
import java.io.IOException;
import java.net.URI;

import dev.vexor.photon.Config;
import dev.vexor.photon.Photon;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.resource.language.I18n;
import net.optifine.gui.GuiScreenOF;
import net.optifine.gui.TooltipManager;
import net.optifine.gui.TooltipProviderEnumShaderOptions;
import net.optifine.shaders.Shaders;
import net.optifine.shaders.ShadersTex;
import net.optifine.shaders.config.EnumShaderOption;
import org.lwjgl.Sys;

public class GuiShaders extends GuiScreenOF {
   protected Screen parentGui;
   protected String screenTitle = "Shaders";
   private TooltipManager tooltipManager = new TooltipManager(this, new TooltipProviderEnumShaderOptions());
   private int updateTimer = -1;
   private GuiSlotShaders shaderList;
   private boolean saved = false;
   private static float[] QUALITY_MULTIPLIERS = new float[]{
      0.5F, 0.6F, 0.6666667F, 0.75F, 0.8333333F, 0.9F, 1.0F, 1.1666666F, 1.3333334F, 1.5F, 1.6666666F, 1.8F, 2.0F
   };
   private static String[] QUALITY_MULTIPLIER_NAMES = new String[]{
      "0.5x", "0.6x", "0.66x", "0.75x", "0.83x", "0.9x", "1x", "1.16x", "1.33x", "1.5x", "1.66x", "1.8x", "2x"
   };
   private static float QUALITY_MULTIPLIER_DEFAULT = 1.0F;
   private static float[] HAND_DEPTH_VALUES = new float[]{0.0625F, 0.125F, 0.25F};
   private static String[] HAND_DEPTH_NAMES = new String[]{"0.5x", "1x", "2x"};
   private static float HAND_DEPTH_DEFAULT = 0.125F;
   public static final int EnumOS_UNKNOWN = 0;
   public static final int EnumOS_WINDOWS = 1;
   public static final int EnumOS_OSX = 2;
   public static final int EnumOS_SOLARIS = 3;
   public static final int EnumOS_LINUX = 4;

   public GuiShaders(Screen par1GuiScreen, GameOptions par2GameSettings) {
      this.parentGui = par1GuiScreen;
   }

   public void init() {
      this.screenTitle = I18n.translate("of.options.shadersTitle", new Object[0]);
      if (Shaders.shadersConfig == null) {
         Shaders.loadConfig();
      }

      int btnWidth = 120;
      int btnHeight = 20;
      int btnX = this.width - btnWidth - 10;
      int baseY = 30;
      int stepY = 20;
      int shaderListWidth = this.width - btnWidth - 20;
      this.shaderList = new GuiSlotShaders(this, shaderListWidth, this.height, baseY, this.height - 50, 16);
      this.shaderList.setButtonIds(7, 8);
      this.buttons.add(new GuiButtonEnumShaderOption(EnumShaderOption.ANTIALIASING, btnX, 0 * stepY + baseY, btnWidth, btnHeight));
      this.buttons.add(new GuiButtonEnumShaderOption(EnumShaderOption.NORMAL_MAP, btnX, 1 * stepY + baseY, btnWidth, btnHeight));
      this.buttons.add(new GuiButtonEnumShaderOption(EnumShaderOption.SPECULAR_MAP, btnX, 2 * stepY + baseY, btnWidth, btnHeight));
      this.buttons.add(new GuiButtonEnumShaderOption(EnumShaderOption.RENDER_RES_MUL, btnX, 3 * stepY + baseY, btnWidth, btnHeight));
      this.buttons.add(new GuiButtonEnumShaderOption(EnumShaderOption.SHADOW_RES_MUL, btnX, 4 * stepY + baseY, btnWidth, btnHeight));
      this.buttons.add(new GuiButtonEnumShaderOption(EnumShaderOption.HAND_DEPTH_MUL, btnX, 5 * stepY + baseY, btnWidth, btnHeight));
      this.buttons.add(new GuiButtonEnumShaderOption(EnumShaderOption.OLD_HAND_LIGHT, btnX, 6 * stepY + baseY, btnWidth, btnHeight));
      this.buttons.add(new GuiButtonEnumShaderOption(EnumShaderOption.OLD_LIGHTING, btnX, 7 * stepY + baseY, btnWidth, btnHeight));
      int btnFolderWidth = Math.min(150, shaderListWidth / 2 - 10);
      int xFolder = shaderListWidth / 4 - btnFolderWidth / 2;
      int yFolder = this.height - 25;
      this.buttons.add(new ButtonWidget(201, xFolder, yFolder, btnFolderWidth - 22 + 1, btnHeight, I18n.translate("of.options.shaders.shadersFolder")));
      this.buttons.add(new GuiButtonDownloadShaders(210, xFolder + btnFolderWidth - 22 - 1, yFolder));
      this.buttons
         .add(
            new ButtonWidget(
               202, shaderListWidth / 4 * 3 - btnFolderWidth / 2, this.height - 25, btnFolderWidth, btnHeight, I18n.translate("gui.done", new Object[0])
            )
         );
      this.buttons.add(new ButtonWidget(203, btnX, this.height - 25, btnWidth, btnHeight, I18n.translate("of.options.shaders.shaderOptions")));
      this.updateButtons();
   }

   public void updateButtons() {
      boolean shaderActive = Config.isShaders();

      for (ButtonWidget button : this.buttons) {
         if (button.id != 201 && button.id != 202 && button.id != 210 && button.id != EnumShaderOption.ANTIALIASING.ordinal()) {
            button.active = shaderActive;
         }
      }
   }

   public void handleMouse() {
      super.handleMouse();
      this.shaderList.handleMouse();
   }

   protected void buttonClicked(ButtonWidget button) {
      this.actionPerformed(button, false);
   }

   @Override
   protected void actionPerformedRightClick(ButtonWidget button) {
      this.actionPerformed(button, true);
   }

   private void actionPerformed(ButtonWidget button, boolean rightClick) {
      if (button.active) {
         if (!(button instanceof GuiButtonEnumShaderOption)) {
            if (!rightClick) {
               switch (button.id) {
                  case 201:
                     switch (getOSType()) {
                        case 1:
                           String var2 = String.format("cmd.exe /C start \"Open file\" \"%s\"", Shaders.shaderPacksDir.getAbsolutePath());

                           try {
                              Runtime.getRuntime().exec(var2);
                              return;
                           } catch (IOException var9) {
                              var9.printStackTrace();
                              break;
                           }
                        case 2:
                           try {
                              Runtime.getRuntime().exec(new String[]{"/usr/bin/open", Shaders.shaderPacksDir.getAbsolutePath()});
                              return;
                           } catch (IOException var10) {
                              var10.printStackTrace();
                           }
                     }

                     boolean var8 = false;

                     try {
                        Class var3 = Class.forName("java.awt.Desktop");
                        Object var4 = var3.getMethod("getDesktop").invoke((Object)null);
                        var3.getMethod("browse", URI.class).invoke(var4, new File(this.client.runDirectory, "shaderpacks").toURI());
                     } catch (Throwable var8x) {
                        var8x.printStackTrace();
                        var8 = true;
                     }

                     if (var8) {
                        Photon.LOGGER.debug("Opening via system class!");
                        Sys.openURL("file://" + Shaders.shaderPacksDir.getAbsolutePath());
                     }
                     break;
                  case 202:
                     Shaders.storeConfig();
                     this.saved = true;
                     this.client.setScreen(this.parentGui);
                     break;
                  case 203:
                     GuiShaderOptions gui = new GuiShaderOptions(this, Config.getGameSettings());
                     Config.getMinecraft().setScreen(gui);
                     break;
                  case 210:
                     try {
                        Class<?> oclass = Class.forName("java.awt.Desktop");
                        Object object = oclass.getMethod("getDesktop").invoke((Object)null);
                        oclass.getMethod("browse", URI.class).invoke(object, new URI("http://optifine.net/shaderPacks"));
                     } catch (Throwable var7) {
                        var7.printStackTrace();
                     }
                  case 204:
                  case 205:
                  case 206:
                  case 207:
                  case 208:
                  case 209:
                  default:
                     this.shaderList.buttonClicked(button);
               }
            }
         } else {
            GuiButtonEnumShaderOption gbeso = (GuiButtonEnumShaderOption)button;
            switch (gbeso.getEnumShaderOption()) {
               case ANTIALIASING:
                  Shaders.nextAntialiasingLevel(!rightClick);
                  if (this.hasShiftDown()) {
                     Shaders.configAntialiasingLevel = 0;
                  }

                  Shaders.uninit();
                  break;
               case NORMAL_MAP:
                  Shaders.configNormalMap = !Shaders.configNormalMap;
                  if (this.hasShiftDown()) {
                     Shaders.configNormalMap = true;
                  }

                  Shaders.uninit();
                  this.client.reloadResourcesConcurrently();
                  break;
               case SPECULAR_MAP:
                  Shaders.configSpecularMap = !Shaders.configSpecularMap;
                  if (this.hasShiftDown()) {
                     Shaders.configSpecularMap = true;
                  }

                  Shaders.uninit();
                  this.client.reloadResourcesConcurrently();
                  break;
               case RENDER_RES_MUL:
                  Shaders.configRenderResMul = this.getNextValue(
                     Shaders.configRenderResMul, QUALITY_MULTIPLIERS, QUALITY_MULTIPLIER_DEFAULT, !rightClick, this.hasShiftDown()
                  );
                  Shaders.uninit();
                  Shaders.scheduleResize();
                  break;
               case SHADOW_RES_MUL:
                  Shaders.configShadowResMul = this.getNextValue(
                     Shaders.configShadowResMul, QUALITY_MULTIPLIERS, QUALITY_MULTIPLIER_DEFAULT, !rightClick, this.hasShiftDown()
                  );
                  Shaders.uninit();
                  Shaders.scheduleResizeShadow();
                  break;
               case HAND_DEPTH_MUL:
                  Shaders.configHandDepthMul = this.getNextValue(
                     Shaders.configHandDepthMul, HAND_DEPTH_VALUES, HAND_DEPTH_DEFAULT, !rightClick, this.hasShiftDown()
                  );
                  Shaders.uninit();
                  break;
               case OLD_HAND_LIGHT:
                  Shaders.configOldHandLight.nextValue(!rightClick);
                  if (this.hasShiftDown()) {
                     Shaders.configOldHandLight.resetValue();
                  }

                  Shaders.uninit();
                  break;
               case OLD_LIGHTING:
                  Shaders.configOldLighting.nextValue(!rightClick);
                  if (this.hasShiftDown()) {
                     Shaders.configOldLighting.resetValue();
                  }

                  Shaders.updateBlockLightLevel();
                  Shaders.uninit();
                  this.client.reloadResourcesConcurrently();
                  break;
               case TWEAK_BLOCK_DAMAGE:
                  Shaders.configTweakBlockDamage = !Shaders.configTweakBlockDamage;
                  break;
               case CLOUD_SHADOW:
                  Shaders.configCloudShadow = !Shaders.configCloudShadow;
                  break;
               case TEX_MIN_FIL_B:
                  Shaders.configTexMinFilB = (Shaders.configTexMinFilB + 1) % 3;
                  Shaders.configTexMinFilN = Shaders.configTexMinFilS = Shaders.configTexMinFilB;
                  button.message = "Tex Min: " + Shaders.texMinFilDesc[Shaders.configTexMinFilB];
                  ShadersTex.updateTextureMinMagFilter();
                  break;
               case TEX_MAG_FIL_N:
                  Shaders.configTexMagFilN = (Shaders.configTexMagFilN + 1) % 2;
                  button.message = "Tex_n Mag: " + Shaders.texMagFilDesc[Shaders.configTexMagFilN];
                  ShadersTex.updateTextureMinMagFilter();
                  break;
               case TEX_MAG_FIL_S:
                  Shaders.configTexMagFilS = (Shaders.configTexMagFilS + 1) % 2;
                  button.message = "Tex_s Mag: " + Shaders.texMagFilDesc[Shaders.configTexMagFilS];
                  ShadersTex.updateTextureMinMagFilter();
                  break;
               case SHADOW_CLIP_FRUSTRUM:
                  Shaders.configShadowClipFrustrum = !Shaders.configShadowClipFrustrum;
                  button.message = "ShadowClipFrustrum: " + toStringOnOff(Shaders.configShadowClipFrustrum);
                  ShadersTex.updateTextureMinMagFilter();
            }

            gbeso.updateButtonText();
         }
      }
   }

   public void removed() {
      super.removed();
      if (!this.saved) {
         Shaders.storeConfig();
      }
   }

   public void render(int mouseX, int mouseY, float tickDelta) {
      this.renderBackground();
      this.shaderList.render(mouseX, mouseY, tickDelta);
      if (this.updateTimer <= 0) {
         this.shaderList.updateList();
         this.updateTimer += 20;
      }

      this.drawCenteredString(this.textRenderer, this.screenTitle + " ", this.width / 2, 15, 16777215);
      String info = "OpenGL: " + Shaders.glVersionString + ", " + Shaders.glVendorString + ", " + Shaders.glRendererString;
      int infoWidth = this.textRenderer.getStringWidth(info);
      if (infoWidth < this.width - 5) {
         this.drawCenteredString(this.textRenderer, info, this.width / 2, this.height - 40, 8421504);
      } else {
         this.drawWithShadow(this.textRenderer, info, 5, this.height - 40, 8421504);
      }

      super.render(mouseX, mouseY, tickDelta);
      this.tooltipManager.drawTooltips(mouseX, mouseY, this.buttons);
   }

   public void tick() {
      super.tick();
      this.updateTimer--;
   }

   public MinecraftClient getMc() {
      return this.client;
   }

   public void drawCenteredString(String text, int x, int y, int color) {
      this.drawCenteredString(this.textRenderer, text, x, y, color);
   }

   public static String toStringOnOff(boolean value) {
      return value ? "On" : "Off";
   }

   public static String toStringAa(int value) {
      if (value == 2) {
         return "FXAA 2x";
      } else {
         return value == 4 ? "FXAA 4x" : "Off";
      }
   }

   public static String toStringValue(float val, float[] values, String[] names) {
      int index = getValueIndex(val, values);
      return names[index];
   }

   private float getNextValue(float val, float[] values, float valDef, boolean forward, boolean reset) {
      if (reset) {
         return valDef;
      } else {
         int index = getValueIndex(val, values);
         if (forward) {
            if (++index >= values.length) {
               index = 0;
            }
         } else if (--index < 0) {
            index = values.length - 1;
         }

         return values[index];
      }
   }

   public static int getValueIndex(float val, float[] values) {
      for (int i = 0; i < values.length; i++) {
         float value = values[i];
         if (value >= val) {
            return i;
         }
      }

      return values.length - 1;
   }

   public static String toStringQuality(float val) {
      return toStringValue(val, QUALITY_MULTIPLIERS, QUALITY_MULTIPLIER_NAMES);
   }

   public static String toStringHandDepth(float val) {
      return toStringValue(val, HAND_DEPTH_VALUES, HAND_DEPTH_NAMES);
   }

   public static int getOSType() {
      String osName = System.getProperty("os.name").toLowerCase();
      if (osName.contains("win")) {
         return 1;
      } else if (osName.contains("mac")) {
         return 2;
      } else if (osName.contains("solaris")) {
         return 3;
      } else if (osName.contains("sunos")) {
         return 3;
      } else if (osName.contains("linux")) {
         return 4;
      } else {
         return osName.contains("unix") ? 4 : 0;
      }
   }

   public static boolean hasShiftDown() {
      return Screen.hasShiftDown();
   }
}

package net.optifine.gui;

import java.awt.Rectangle;
import java.util.Arrays;
import java.util.List;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;

public class TooltipManager {
    private Screen guiScreen;
    private TooltipProvider tooltipProvider;
    private int lastMouseX = 0;
    private int lastMouseY = 0;
    private long mouseStillTime = 0L;

    public TooltipManager(Screen guiScreen, TooltipProvider tooltipProvider) {
        this.guiScreen = guiScreen;
        this.tooltipProvider = tooltipProvider;
    }

    public void drawTooltips(int x, int y, List buttonList) {
        if (Math.abs(x - this.lastMouseX) <= 5 && Math.abs(y - this.lastMouseY) <= 5) {
            int activateDelay = 700;
            if (System.currentTimeMillis() >= this.mouseStillTime + (long)activateDelay) {
                ButtonWidget btn = GuiScreenOF.getSelectedButton(x, y, buttonList);
                if (btn != null) {
                    Rectangle rect = this.tooltipProvider.getTooltipBounds(this.guiScreen, x, y);
                    String[] lines = this.tooltipProvider.getTooltipLines(btn, rect.width);
                    if (lines != null) {
                        if (lines.length > 8) {
                            lines = Arrays.copyOf(lines, 8);
                            lines[lines.length - 1] = lines[lines.length - 1] + " ...";
                        }

                        if (this.tooltipProvider.isRenderBorder()) {
                            int colBorder = -528449408;
                            this.drawRectBorder(rect.x, rect.y, rect.x + rect.width, rect.y + rect.height, colBorder);
                        }

                        DrawableHelper.fill(rect.x, rect.y, rect.x + rect.width, rect.y + rect.height, -536870912);

                        for (int i = 0; i < lines.length; i++) {
                            String line = lines[i];
                            int col = 14540253;
                            if (line.endsWith("!")) {
                                col = 16719904;
                            }

                            TextRenderer fontRenderer = MinecraftClient.getInstance().textRenderer;
                            fontRenderer.drawWithShadow(line, (float)(rect.x + 5), (float)(rect.y + 5 + i * 11), col);
                        }
                    }
                }
            }
        } else {
            this.lastMouseX = x;
            this.lastMouseY = y;
            this.mouseStillTime = System.currentTimeMillis();
        }
    }

    private void drawRectBorder(int x1, int y1, int x2, int y2, int col) {
        DrawableHelper.fill(x1, y1 - 1, x2, y1, col);
        DrawableHelper.fill(x1, y2, x2, y2 + 1, col);
        DrawableHelper.fill(x1 - 1, y1, x1, y2, col);
        DrawableHelper.fill(x2, y1, x2 + 1, y2, col);
    }
}

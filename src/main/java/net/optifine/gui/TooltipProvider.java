package net.optifine.gui;

import java.awt.Rectangle;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;

public interface TooltipProvider {
    Rectangle getTooltipBounds(Screen var1, int var2, int var3);

    String[] getTooltipLines(ButtonWidget var1, int var2);

    boolean isRenderBorder();
}

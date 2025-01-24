package net.optifine.gui;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.option.GameOptions.Option;
import net.minecraft.client.resource.language.I18n;

public class TooltipProviderOptions implements TooltipProvider {
    @Override
    public Rectangle getTooltipBounds(Screen guiScreen, int x, int y) {
        int x1 = guiScreen.width / 2 - 150;
        int y1 = guiScreen.height / 6 - 7;
        if (y <= y1 + 98) {
            y1 += 105;
        }

        int x2 = x1 + 150 + 150;
        int y2 = y1 + 84 + 10;
        return new Rectangle(x1, y1, x2 - x1, y2 - y1);
    }

    @Override
    public boolean isRenderBorder() {
        return false;
    }

    @Override
    public String[] getTooltipLines(ButtonWidget btn, int width) {
        if (!(btn instanceof IOptionControl)) {
            return null;
        } else {
            IOptionControl ctl = (IOptionControl)btn;
            Option option = ctl.getOption();
            return getTooltipLines(option.getName());
        }
    }

    public static String[] getTooltipLines(String key) {
        List<String> list = new ArrayList<>();

        for (int i = 0; i < 10; i++) {
            String lineKey = key + ".tooltip." + (i + 1);
            String line = I18n.translate(lineKey, null);
            if (line == null) {
                break;
            }

            list.add(line);
        }

        return list.size() <= 0 ? null : list.toArray(new String[list.size()]);
    }
}

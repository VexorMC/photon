package net.optifine.gui;

import java.io.IOException;
import java.util.List;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.VideoOptionsScreen;
import net.minecraft.client.gui.widget.ButtonWidget;

public class GuiScreenOF extends Screen {
    protected void actionPerformedRightClick(ButtonWidget button) {
    }

    protected void mouseClicked(int mouseX, int mouseY, int button)  {
        super.mouseClicked(mouseX, mouseY, button);
        if (button == 1) {
            ButtonWidget btn = getSelectedButton(mouseX, mouseY, this.buttons);
            if (btn != null && btn.active) {
                btn.playDownSound(this.client.getSoundManager());
                this.actionPerformedRightClick(btn);
            }
        }
    }

    public static ButtonWidget getSelectedButton(int x, int y, List<ButtonWidget> listButtons) {
        for (int i = 0; i < listButtons.size(); i++) {
            ButtonWidget btn = listButtons.get(i);
            if (btn.visible) {
                int btnWidth = 200;
                int btnHeight = 20;
                if (x >= btn.x && y >= btn.y && x < btn.x + btnWidth && y < btn.y + btnHeight) {
                    return btn;
                }
            }
        }

        return null;
    }
}

package dev.vexor.photon.mixin;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.VideoOptionsScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.optifine.Lang;
import net.optifine.shaders.gui.GuiShaders;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.awt.*;

@Mixin(VideoOptionsScreen.class)
public class MixinVideoOptionsScreen extends Screen {
    @Inject(method = "init", at = @At("RETURN"))
    void impl$init(CallbackInfo ci) {
        this.buttons.add(new ButtonWidget(69420, 20, 20, Lang.get("Shaders")));
    }

    @Inject(method = "buttonClicked", at = @At("HEAD"))
    void impl$buttonClicked(ButtonWidget button, CallbackInfo ci) {
        if (button.id == 69420) {
            this.client.setScreen(new GuiShaders(this.client.currentScreen, this.client.options));
        }
    }
}

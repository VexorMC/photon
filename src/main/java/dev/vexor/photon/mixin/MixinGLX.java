package dev.vexor.photon.mixin;

import com.mojang.blaze3d.platform.GLX;
import net.minecraft.client.MinecraftClient;
import net.optifine.shaders.Shaders;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GLX.class)
public class MixinGLX {
    @Inject(method = "createContext", at = @At("HEAD"))
    private static void impl$createContext(CallbackInfo ci) {
        Shaders.startup(MinecraftClient.getInstance());
    }
}

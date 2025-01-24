package dev.vexor.photon.mixin;

import com.mojang.blaze3d.platform.GlStateManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(GlStateManager.class)
public class MixinGlStateManager {
    @ModifyConstant(method = "<clinit>", constant = @Constant(intValue = 8), require = 1)
    private static int increaseTextureSlots(int existingValue) {
        return 32;
    }
}
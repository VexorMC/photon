package dev.vexor.photon.mixin;

import dev.vexor.photon.Config;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.optifine.shaders.Shaders;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HeldItemRenderer.class)
public class MixinHeldItemRenderer {
    @Shadow @Final private MinecraftClient client;

    @Inject(method = "renderArmHoldingItem", at = @At("HEAD"), cancellable = true)
    void impl$renderArmHoldingItem(CallbackInfo ci) {
        if (Config.isShaders() || Shaders.isSkipRenderHand()) ci.cancel();
    }

    @Inject(method = "renderUnderwaterOverlay", at = @At("HEAD"), cancellable = true)
    void impl$renderUnderwaterOverlay(CallbackInfo ci) {
        if (Config.isShaders() || !Shaders.isUnderwaterOverlay()) ci.cancel();
    }

    @Inject(method = "updateHeldItems", at = @At(value = "FIELD", target = "Lnet/minecraft/client/render/item/HeldItemRenderer;selectedSlot:I"))
    void impl$updateHeldItems(CallbackInfo ci) {
        PlayerEntity playerEntity = this.client.player;
        ItemStack itemStack = playerEntity.inventory.getMainHandStack();
        if (Config.isShaders()) Shaders.setItemToRenderMain(itemStack);
    }
}

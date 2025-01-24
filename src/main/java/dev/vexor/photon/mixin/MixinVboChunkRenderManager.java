package dev.vexor.photon.mixin;

import dev.vexor.photon.Config;
import net.minecraft.client.render.world.VboChunkRenderManager;
import net.optifine.shaders.ShadersRender;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(VboChunkRenderManager.class)
public abstract class MixinVboChunkRenderManager {
    @Shadow protected abstract void method_9929();

    @Redirect(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/world/VboChunkRenderManager;method_9929()V"))
    void impl$render(VboChunkRenderManager instance) {
        if (Config.isShaders()) {
            ShadersRender.setupArrayPointersVbo();
        } else {
            this.method_9929();
        }
    }
}

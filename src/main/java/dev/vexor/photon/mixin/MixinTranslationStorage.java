package dev.vexor.photon.mixin;

import net.minecraft.client.resource.language.TranslationStorage;
import net.minecraft.resource.Resource;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.IOException;
import java.util.List;

@Mixin(TranslationStorage.class)
public abstract class MixinTranslationStorage {
    @Shadow
    protected abstract void load(List<Resource> resources);

    @Inject(method = "load(Lnet/minecraft/resource/ResourceManager;Ljava/util/List;)V", at = @At("RETURN"))
    public void load(ResourceManager resourceManager, List<String> languages, CallbackInfo ci) {
        for (String code : languages) {
            String path = String.format("optifine/lang/%s.lang", code);

            try {
                this.load(resourceManager.getAllResources(new Identifier(path)));
            } catch (IOException ignored) {
            }
        }
    }
}

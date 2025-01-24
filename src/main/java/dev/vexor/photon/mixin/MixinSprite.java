package dev.vexor.photon.mixin;

import dev.vexor.photon.Config;
import dev.vexor.photon.tex.ExtendedSprite;
import net.minecraft.client.texture.Sprite;
import net.minecraft.util.Identifier;
import net.optifine.shaders.Shaders;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(Sprite.class)
public class MixinSprite implements ExtendedSprite {
    @Shadow @Final private String name;
    @Shadow protected List<int[][]> frames;
    @Unique public boolean isShadersSprite = false;
    @Unique public Sprite spriteNormal, spriteSpecular;

    @Inject(method = "reInitialize", at = @At("RETURN"))
    void impl$reInitialize(CallbackInfo ci) {
        if (spriteNormal != null) {
            spriteNormal.copyData((Sprite) (Object) this);
        }
        if (spriteSpecular != null) {
            spriteSpecular.copyData((Sprite) (Object) this);
        }
    }

    @Inject(method = "method_7009", at = @At("RETURN"))
    void impl$loadSprites(CallbackInfo ci) {
        if (!this.isShadersSprite)
        {
            if (Config.isShaders())
            {
                this.loadShadersSprites();
            }

            for (int k1 = 0; k1 < this.frames.size(); ++k1)
            {
                int[][] aint1 = (int[][])((int[][])this.frames.get(k1));

                if (aint1 != null && !this.name.startsWith("minecraft:blocks/leaves_"))
                {
                    for (int i2 = 0; i2 < aint1.length; ++i2)
                    {
                        int[] aint2 = aint1[i2];
                        this.fixTransparentColor(aint2);
                    }
                }
            }
        }
    }

    @Unique
    private void fixTransparentColor(int[] p_fixTransparentColor_1_)
    {
        if (p_fixTransparentColor_1_ != null)
        {
            long i = 0L;
            long j = 0L;
            long k = 0L;
            long l = 0L;

            for (int i1 = 0; i1 < p_fixTransparentColor_1_.length; ++i1)
            {
                int j1 = p_fixTransparentColor_1_[i1];
                int k1 = j1 >> 24 & 255;

                if (k1 >= 16)
                {
                    int l1 = j1 >> 16 & 255;
                    int i2 = j1 >> 8 & 255;
                    int j2 = j1 & 255;
                    i += (long)l1;
                    j += (long)i2;
                    k += (long)j2;
                    ++l;
                }
            }

            if (l > 0L)
            {
                int l2 = (int)(i / l);
                int i3 = (int)(j / l);
                int j3 = (int)(k / l);
                int k3 = l2 << 16 | i3 << 8 | j3;

                for (int l3 = 0; l3 < p_fixTransparentColor_1_.length; ++l3)
                {
                    int i4 = p_fixTransparentColor_1_[l3];
                    int k2 = i4 >> 24 & 255;

                    if (k2 <= 16)
                    {
                        p_fixTransparentColor_1_[l3] = k3;
                    }
                }
            }
        }
    }

    @Unique
    private void loadShadersSprites() {
        if (Shaders.configNormalMap) {
            String s = this.name + "_n";
            Identifier resourcelocation = new Identifier(s);
            resourcelocation = Config.getTextureMap().method_7003(resourcelocation, 0);

            if (Config.hasResource(resourcelocation)) {
                this.spriteNormal = new Sprite(s);
                ((ExtendedSprite) this.spriteNormal).setShaderSprite(true);
                this.spriteNormal.copyData((Sprite) (Object) this);
                this.spriteNormal.method_7013(512);
            }
        }

        if (Shaders.configSpecularMap) {
            String s1 = this.name + "_s";
            Identifier resourcelocation1 = new Identifier(s1);
            resourcelocation1 = Config.getTextureMap().method_7003(resourcelocation1, 0);

            if (Config.hasResource(resourcelocation1)) {
                this.spriteSpecular = new Sprite(s1);
                ((ExtendedSprite) this.spriteNormal).setShaderSprite(true);
                this.spriteSpecular.copyData((Sprite) (Object) this);
                this.spriteSpecular.method_7013(512);
            }
        }
    }

    @Override
    public boolean isShaderSprite() {
        return this.isShadersSprite;
    }

    @Override
    public void setShaderSprite(boolean value) {
        this.isShadersSprite = value;
    }

    @Override
    public Sprite getNormalSprite() {
        return this.spriteNormal;
    }

    @Override
    public void setNormalSprite(Sprite value) {
        this.spriteNormal = value;
    }

    @Override
    public Sprite getSpecularSprite() {
        return this.spriteSpecular;
    }

    @Override
    public void setSpecularSprite(Sprite value) {
        this.spriteSpecular = value;
    }
}

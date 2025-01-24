package dev.vexor.photon.tex;

import net.minecraft.client.texture.Sprite;

public interface ExtendedSprite {
    boolean isShaderSprite();
    void setShaderSprite(boolean value);

    Sprite getNormalSprite();
    void setNormalSprite(Sprite value);

    Sprite getSpecularSprite();
    void setSpecularSprite(Sprite value);
}

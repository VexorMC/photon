package dev.vexor.photon.tex;

import net.optifine.shaders.MultiTexID;

public interface ExtendedTexture {
    MultiTexID getMultiTex();

    MultiTexID getMultiTexDirect();
    void setMultiTex(MultiTexID id);
}

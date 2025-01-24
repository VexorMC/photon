package net.optifine.util;

import com.mojang.blaze3d.platform.GlStateManager;
import dev.vexor.photon.Config;
import net.optifine.shaders.Shaders;
import org.lwjgl.opengl.GL11;

import java.nio.IntBuffer;

public class PhotonSM {
    public static void deleteTextures(IntBuffer p_deleteTextures_0_)
    {
        p_deleteTextures_0_.rewind();

        while (p_deleteTextures_0_.position() < p_deleteTextures_0_.limit())
        {
            int i = p_deleteTextures_0_.get();
            GlStateManager.deleteTexture(i);
        }

        p_deleteTextures_0_.rewind();
    }


    public static void glDrawArrays(int p_glDrawArrays_0_, int p_glDrawArrays_1_, int p_glDrawArrays_2_)
    {
        GL11.glDrawArrays(p_glDrawArrays_0_, p_glDrawArrays_1_, p_glDrawArrays_2_);

        if (Config.isShaders())
        {
            int i = Shaders.activeProgram.getCountInstances();

            if (i > 1)
            {
                for (int j = 1; j < i; ++j)
                {
                    Shaders.uniform_instanceId.setValue(j);
                    GL11.glDrawArrays(p_glDrawArrays_0_, p_glDrawArrays_1_, p_glDrawArrays_2_);
                }

                Shaders.uniform_instanceId.setValue(0);
            }
        }
    }
}

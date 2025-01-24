package dev.vexor.photon.mixin;

import com.mojang.blaze3d.platform.GLX;
import com.mojang.blaze3d.platform.GlStateManager;
import dev.vexor.photon.Config;
import dev.vexor.photon.tex.ExtendedBufferBuilder;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormatElement;
import net.optifine.shaders.SVertexBuilder;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.nio.ByteBuffer;
import java.util.List;

@Mixin(BufferRenderer.class)
public class MixinBufferRenderer {
    /**
     * @reason Shaders
     * @author Lunasa
     */
    @Overwrite
    public void draw(BufferBuilder builder) {
        if (builder.getVertexCount() > 0) {
            if (builder.getDrawMode() == 7 && Config.isQuadsToTriangles())
            {
                ((ExtendedBufferBuilder)builder).quadsToTriangles();
            }

            VertexFormat vertexFormat = builder.getFormat();
            int i = vertexFormat.getVertexSize();
            ByteBuffer byteBuffer = builder.getByteBuffer();
            List<VertexFormatElement> list = vertexFormat.getElements();

            for(int j = 0; j < list.size(); ++j) {
                VertexFormatElement vertexFormatElement = (VertexFormatElement)list.get(j);
                VertexFormatElement.Type type = vertexFormatElement.getType();
                int k = vertexFormatElement.getFormat().getGlId();
                int l = vertexFormatElement.getIndex();
                byteBuffer.position(vertexFormat.getIndex(j));
                switch (type) {
                    case POSITION:
                        GL11.glVertexPointer(vertexFormatElement.getCount(), k, i, byteBuffer);
                        GL11.glEnableClientState(32884);
                        break;
                    case UV:
                        GLX.gl13ClientActiveTexture(GLX.textureUnit + l);
                        GL11.glTexCoordPointer(vertexFormatElement.getCount(), k, i, byteBuffer);
                        GL11.glEnableClientState(32888);
                        GLX.gl13ClientActiveTexture(GLX.textureUnit);
                        break;
                    case COLOR:
                        GL11.glColorPointer(vertexFormatElement.getCount(), k, i, byteBuffer);
                        GL11.glEnableClientState(32886);
                        break;
                    case NORMAL:
                        GL11.glNormalPointer(k, i, byteBuffer);
                        GL11.glEnableClientState(32885);
                }
            }

            if (Config.isShaders())
            {
                SVertexBuilder.drawArrays(builder.getDrawMode(), 0, builder.getVertexCount(), builder);
            }
            else
            {
                GL11.glDrawArrays(builder.getDrawMode(), 0, builder.getVertexCount());
            }

            int j = 0;

            for(int m = list.size(); j < m; ++j) {
                VertexFormatElement vertexFormatElement2 = (VertexFormatElement)list.get(j);
                VertexFormatElement.Type type2 = vertexFormatElement2.getType();
                int l = vertexFormatElement2.getIndex();
                switch (type2) {
                    case POSITION:
                        GL11.glDisableClientState(32884);
                        break;
                    case UV:
                        GLX.gl13ClientActiveTexture(GLX.textureUnit + l);
                        GL11.glDisableClientState(32888);
                        GLX.gl13ClientActiveTexture(GLX.textureUnit);
                        break;
                    case COLOR:
                        GL11.glDisableClientState(32886);
                        GlStateManager.clearColor();
                        break;
                    case NORMAL:
                        GL11.glDisableClientState(32885);
                }
            }
        }

        builder.reset();
    }
}

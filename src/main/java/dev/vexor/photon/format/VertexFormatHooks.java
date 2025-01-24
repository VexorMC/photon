package dev.vexor.photon.format;

import dev.vexor.photon.Config;
import dev.vexor.photon.mixin.VertexFormatsAccessor;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.optifine.shaders.SVertexFormat;

public class VertexFormatHooks {
    private static final VertexFormat BLOCK_VANILLA = VertexFormats.BLOCK;
    private static final VertexFormat ITEM_VANILLA = VertexFormats.BLOCK_NORMALS;

    public static void init() {
        // dummy to call static ctor
    }

    public static void updateVertexFormats() {
        if (Config.isShaders()) {
            VertexFormatsAccessor.setBLOCK(SVertexFormat.makeDefVertexFormatBlock());
            VertexFormatsAccessor.setBLOCK_NORMALS(SVertexFormat.makeDefVertexFormatItem());
        } else {
            VertexFormatsAccessor.setBLOCK(BLOCK_VANILLA);
            VertexFormatsAccessor.setBLOCK_NORMALS(ITEM_VANILLA);
        }
    }
}

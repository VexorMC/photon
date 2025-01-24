package dev.vexor.photon.mixin;

import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(VertexFormats.class)
public interface VertexFormatsAccessor {
    @Accessor @Mutable
    static void setBLOCK(VertexFormat vertexFormat) { throw new UnsupportedOperationException(); }
    @Accessor @Mutable
    static void setBLOCK_NORMALS(VertexFormat vertexFormat) { throw new UnsupportedOperationException(); }
}

package dev.vexor.photon.mixin;

import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

@Mixin(VertexFormats.class)
public class MixinVertexFormats {
    @Unique
    private static final VertexFormat BLOCK_VANILLA = VertexFormats.BLOCK;
    @Unique
    private static final VertexFormat ITEM_VANILLA = VertexFormats.BLOCK_NORMALS;
}

package dev.vexor.photon.mixin;

import dev.vexor.photon.Config;
import dev.vexor.photon.tex.ExtendedBufferBuilder;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.util.GlAllocationUtils;
import net.optifine.shaders.SVertexBuilder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

@Mixin(BufferBuilder.class)
public abstract class MixinBufferBuilder implements ExtendedBufferBuilder {
    @Shadow private int drawMode;
    @Shadow private ByteBuffer buffer;

    private boolean modeTriangles = false;

    @Shadow public abstract VertexFormat getFormat();

    @Shadow public IntBuffer intBuffer;
    @Shadow private int vertexCount;
    @Unique private SVertexBuilder vertexBuilder;
    @Unique private ByteBuffer bufferTriangles;

    private boolean[] drawnIcons = new boolean[256];
    private Sprite[] quadSprites = null;
    private Sprite[] quadSpritesPrev = null;
    private Sprite quadSprite = null;

    @Inject(method = "<init>", at = @At("RETURN"))
    void impl$init(CallbackInfo ci) {
        SVertexBuilder.initVertexBuilder((BufferBuilder)(Object)this);
    }

    @Inject(method = "begin", at = @At(value = "INVOKE", target = "Ljava/nio/ByteBuffer;limit(I)Ljava/nio/Buffer;"))
    void impl$begin(CallbackInfo ci) {
        if (Config.isShaders()) SVertexBuilder.endSetVertexFormat((BufferBuilder)(Object)this);
    }

    @Inject(method = "putArray", at = @At(value = "HEAD"))
    void impl$putArray$begin(int[] data, CallbackInfo ci) {
        if (Config.isShaders()) SVertexBuilder.beginAddVertexData((BufferBuilder)(Object)this, data);
    }

    @Inject(method = "putArray", at = @At(value = "RETURN"))
    void impl$putArray$end(int[] data, CallbackInfo ci) {
        if (Config.isShaders()) SVertexBuilder.endAddVertexData((BufferBuilder) (Object) this);
    }

    @Inject(method = "next", at = @At(value = "RETURN"))
    void impL$next(CallbackInfo ci) {
        if (Config.isShaders()) SVertexBuilder.endAddVertex((BufferBuilder)(Object)this);
    }

    @Inject(method = "vertex", at = @At(value = "HEAD"))
    void impl$vertex(double x, double y, double z, CallbackInfoReturnable<BufferBuilder> cir) {
        if (Config.isShaders()) SVertexBuilder.beginAddVertex((BufferBuilder)(Object)this);
    }

    @Override
    public SVertexBuilder getVertexBuilder() {
        return this.vertexBuilder;
    }

    @Override
    public void setVertexBuilder(SVertexBuilder vertexBuilder) {
        this.vertexBuilder = vertexBuilder;
    }

    @Override
    public void quadsToTriangles() {
        if (this.drawMode == 7) {
            if (this.bufferTriangles == null) this.bufferTriangles = GlAllocationUtils.allocateByteBuffer(this.buffer.capacity() * 2);
            if (this.bufferTriangles.capacity() < this.buffer.capacity() * 2) this.bufferTriangles = GlAllocationUtils.allocateByteBuffer(this.buffer.capacity() * 2);
            final int i = this.getFormat().getVertexSize();
            final int j = this.buffer.limit();
            this.buffer.rewind();
            this.bufferTriangles.clear();
            for (int k = 0; k < this.getVertexCount(); k += 4) {
                this.buffer.limit((k + 3) * i);
                this.buffer.position(k * i);
                this.bufferTriangles.put(this.buffer);
                this.buffer.limit((k + 1) * i);
                this.buffer.position(k * i);
                this.bufferTriangles.put(this.buffer);
                this.buffer.limit((k + 2 + 2) * i);
                this.buffer.position((k + 2) * i);
                this.bufferTriangles.put(this.buffer);
            }
            this.buffer.limit(j);
            this.buffer.rewind();
            this.bufferTriangles.flip();
            this.modeTriangles = true;
        }
    }

    @Inject(method = "reset", at = @At("RETURN"))
    void impl$reset(CallbackInfo ci) {
        this.modeTriangles = false;
    }

    @Overwrite public ByteBuffer getByteBuffer() { return this.modeTriangles ? this.bufferTriangles : this.buffer; }

    @Overwrite public int getVertexCount() { return this.modeTriangles ? this.vertexCount / 4 * 6 : this.vertexCount; }

    @Overwrite public int getDrawMode() { return this.modeTriangles ? 4 : this.drawMode; }
}

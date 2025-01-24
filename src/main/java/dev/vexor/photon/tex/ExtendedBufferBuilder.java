package dev.vexor.photon.tex;

import net.optifine.shaders.SVertexBuilder;

public interface ExtendedBufferBuilder {
    SVertexBuilder getVertexBuilder();
    void setVertexBuilder(SVertexBuilder vertexBuilder);

    void quadsToTriangles();
}

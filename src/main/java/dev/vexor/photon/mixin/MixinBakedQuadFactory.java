package dev.vexor.photon.mixin;

import dev.vexor.photon.Config;
import net.minecraft.client.render.model.BakedQuadFactory;
import net.minecraft.client.render.model.ModelRotation;
import net.minecraft.client.render.model.json.ModelElementFace;
import net.minecraft.client.texture.Sprite;
import net.minecraft.util.math.Direction;
import net.optifine.shaders.Shaders;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(BakedQuadFactory.class)
public abstract class MixinBakedQuadFactory {
    @Shadow protected abstract void packVertexData(int[] vertices, int cornerIndex, Direction direction, ModelElementFace face, float[] positionMatrix, Sprite sprite, ModelRotation rotation, net.minecraft.client.render.model.json.ModelRotation rotation2, boolean lockUv, boolean shade);

    /**
     * @reason Shaders
     * @author Lunasa
     */
    @Overwrite
    private int[] packVertexData(ModelElementFace face, Sprite sprite, Direction dir, float[] matrix, ModelRotation rotation1, net.minecraft.client.render.model.json.ModelRotation rotation2, boolean lockUv, boolean shade) {
        int a = 28;
        if (Config.isShaders()) {
            a = 56;
        }
        int[] is = new int[a];
        for (int i = 0; i < 4; ++i) {
            this.packVertexData(is, i, dir, face, matrix, sprite, rotation1, rotation2, lockUv, shade);
        }

        return is;
    }

    /**
     * @reason Shaders
     * @author Lunasa
     */
    @Overwrite
    private float getBrightness(Direction dir) {
        switch (dir) {
            case DOWN:
                if (Config.isShaders()) {
                    return Shaders.blockLightLevel05;
                }
                return 0.5F;
            case UP:
                return 1.0F;
            case NORTH:
            case SOUTH:
                if (Config.isShaders()) {
                    return Shaders.blockLightLevel08;
                }
                return 0.8F;
            case WEST:
            case EAST:
                if (Config.isShaders()) {
                    return Shaders.blockLightLevel06;
                }
                return 0.6F;
            default:
                return 1.0F;
        }
    }
}

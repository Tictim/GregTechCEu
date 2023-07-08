package gregtech.client.model.component;

import gregtech.client.utils.MatrixUtils;
import net.minecraft.util.EnumFacing;
import org.lwjgl.util.vector.Matrix3f;
import org.lwjgl.util.vector.Vector2f;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.function.Consumer;

public final class ComponentTexture {

    private final String texture;
    private final int tintIndex;

    @Nullable
    private Matrix3f uvTransformation;

    private boolean bloom;

    public ComponentTexture(String texture, int tintIndex) {
        this.texture = texture;
        this.tintIndex = tintIndex;
    }

    @Nonnull
    public String texture() {
        return texture;
    }

    public int tintIndex() {
        return tintIndex;
    }

    @Nullable
    public Matrix3f getUVTransformation() {
        return uvTransformation;
    }

    public boolean isBloom() {
        return bloom;
    }

    @Nonnull
    public Vector2f[] getUVs(@Nonnull ComponentShape shape, @Nonnull EnumFacing facing) {
        float u1 = switch (facing) {
            case DOWN, UP, SOUTH -> shape.fromX();
            case NORTH -> 16.0F - shape.toX();
            case WEST -> shape.fromZ();
            case EAST -> 16.0F - shape.toZ();
        };

        float v1 = switch (facing) {
            case DOWN -> 16.0F - shape.toZ();
            case UP -> shape.fromZ();
            case NORTH, SOUTH, WEST, EAST -> 16.0F - shape.toY();
        };

        float u2 = switch (facing) {
            case DOWN, UP, SOUTH -> shape.toX();
            case NORTH -> 16.0F - shape.fromX();
            case WEST -> shape.toZ();
            case EAST -> 16.0F - shape.fromZ();
        };

        float v2 = switch (facing) {
            case DOWN -> 16.0F - shape.fromZ();
            case UP -> shape.toZ();
            case NORTH, SOUTH, WEST, EAST -> 16.0F - shape.fromY();
        };

        // vanilla logic, don't question
        float adjustedU1 = (float) (u1 * 0.999 + u2 * 0.001);
        float adjustedU2 = (float) (u1 * 0.001 + u2 * 0.999);
        float adjustedV1 = (float) (v1 * 0.999 + v2 * 0.001);
        float adjustedV2 = (float) (v1 * 0.001 + v2 * 0.999);

        Vector2f[] uvs = {
                new Vector2f(adjustedU1, adjustedV1),
                new Vector2f(adjustedU1, adjustedV2),
                new Vector2f(adjustedU2, adjustedV2),
                new Vector2f(adjustedU2, adjustedV1)
        };

        if (this.uvTransformation != null) {
            for (Vector2f uv : uvs) {
                MatrixUtils.transform(this.uvTransformation, uv, uv);
            }
        }

        return uvs;
    }

    @Nonnull
    public ComponentTexture setUVTransformation(@Nonnull Consumer<Matrix3f> function) {
        Matrix3f mat = new Matrix3f();
        function.accept(mat);
        this.uvTransformation = mat;
        return this;
    }

    @Nonnull
    public ComponentTexture setBloom(boolean value) {
        this.bloom = value;
        return this;
    }

    @Override
    public String toString() {
        return "ComponentTexture{" +
                "texture='" + texture + '\'' +
                ", tintIndex=" + tintIndex +
                ", uvTransformation=" + uvTransformation +
                ", bloom=" + bloom +
                '}';
    }
}

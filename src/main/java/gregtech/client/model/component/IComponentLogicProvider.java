package gregtech.client.model.component;

import gregtech.client.utils.MatrixUtils;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import org.lwjgl.util.vector.Matrix4f;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

@FunctionalInterface
public interface IComponentLogicProvider {

    String TEXTURE_PARTICLE = "#particle";

    @Nonnull
    IComponentLogic buildLogic(@Nonnull ComponentModel.Register componentRegister, @Nonnull ModelTextureMapping textureMapping);

    default boolean defaultAmbientOcclusion() {
        return true;
    }

    default boolean defaultGui3d() {
        return true;
    }

    default boolean defaultUVLock() {
        return false;
    }

    @Nonnull
    default ModelTextureMapping getDefaultTextureMappings() {
        return ModelTextureMapping.empty();
    }

    @Nullable
    default Matrix4f getCameraTransform(@Nonnull ItemCameraTransforms.TransformType type) {
        return getBlockCameraTransform(type);
    }

    @Nullable
    static Matrix4f getBlockCameraTransform(@Nonnull ItemCameraTransforms.TransformType type) {
        if (type == ItemCameraTransforms.TransformType.HEAD || type == ItemCameraTransforms.TransformType.NONE) {
            return null;
        }

        Matrix4f mat = new Matrix4f();

        switch (type) {
            case GUI -> {
                MatrixUtils.rotate(mat, 30, 225, 0);
                MatrixUtils.scale(mat, .625f);
            }
            case GROUND -> {
                MatrixUtils.translate(mat, 0, 3, 0);
                MatrixUtils.scale(mat, .25f);
            }
            case FIXED -> {
                MatrixUtils.scale(mat, .5f);
            }
            case THIRD_PERSON_LEFT_HAND, THIRD_PERSON_RIGHT_HAND -> {
                MatrixUtils.translate(mat, 0, 2.5f, 0);
                MatrixUtils.rotate(mat, 75, 45, 0);
                MatrixUtils.scale(mat, .375f);
            }
            case FIRST_PERSON_RIGHT_HAND -> {
                MatrixUtils.rotate(mat, 0, 45, 0);
                MatrixUtils.scale(mat, .4f);
            }
            case FIRST_PERSON_LEFT_HAND -> {
                MatrixUtils.rotate(mat, 0, 225, 0);
                MatrixUtils.scale(mat, .4f);
            }
        }
        return mat;
    }
}

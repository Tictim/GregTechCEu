package gregtech.client.model.component;

import javax.annotation.Nonnull;

@FunctionalInterface
public interface IComponentLogicProvider {

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
        return ModelTextureMapping.EMPTY;
    }
}

package gregtech.client.model.component;

import gregtech.client.model.ISpecialBakedModel;
import gregtech.common.blocks.special.ISpecialState;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class BakedComponentModel implements ISpecialBakedModel {

    private final BakedComponent components;
    private final IComponentLogic componentLogic;
    private final TextureAtlasSprite particleTexture;
    private final boolean ambientOcclusion;
    private final boolean gui3d;

    public BakedComponentModel(@Nonnull BakedComponent components,
                               @Nonnull IComponentLogic componentLogic,
                               @Nonnull TextureAtlasSprite particleTexture,
                               boolean ambientOcclusion,
                               boolean gui3d) {
        this.components = components;
        this.componentLogic = componentLogic;
        this.particleTexture = particleTexture;
        this.ambientOcclusion = ambientOcclusion;
        this.gui3d = gui3d;
    }

    @Nonnull
    @Override
    public ModelStates collectModels(@Nullable ISpecialState state) {
        ModelStates collector = new ModelStates(state, this.components, isBloomActive());
        this.componentLogic.computeStates(collector, state != null ? new WorldContext(state) : null);
        return collector;
    }

    @Override
    public boolean isBloomActive() {
        return this.componentLogic.isBloomActive();
    }

    @Override
    public boolean isAmbientOcclusion() {
        return this.ambientOcclusion;
    }

    @Override
    public boolean isAmbientOcclusion(@Nonnull IBlockState state) {
        return this.ambientOcclusion;
    }

    @Override
    public boolean isGui3d() {
        return this.gui3d;
    }

    @Nonnull
    @Override
    public TextureAtlasSprite getParticleTexture() {
        return this.particleTexture;
    }
}

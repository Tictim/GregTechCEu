package gregtech.client.model.special;

import gregtech.common.blocks.special.ISpecialState;
import gregtech.integration.ctm.ISpecialBakedModel;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ItemOverrideList;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.EnumFacing;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

public final class SpecialBakedModel implements ISpecialBakedModel {

    private final List<IBakedModel> parts;
    private final IModelLogic modelLogic;
    private final TextureAtlasSprite particleTexture;
    private final boolean ambientOcclusion;
    private final boolean gui3d;

    public SpecialBakedModel(@Nonnull List<IBakedModel> parts,
                             @Nonnull IModelLogic modelLogic,
                             @Nonnull TextureAtlasSprite particleTexture,
                             boolean ambientOcclusion,
                             boolean gui3d) {
        this.parts = parts;
        this.modelLogic = modelLogic;
        this.particleTexture = particleTexture;
        this.ambientOcclusion = ambientOcclusion;
        this.gui3d = gui3d;
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable IBlockState state, @Nullable EnumFacing side, long rand) {
        if (state instanceof ISpecialState specialState) {
            ModelCollector modelStateCache = specialState.getModelStateCache();
            if (modelStateCache == null) {
                modelStateCache = collectModels(specialState);
                specialState.setModelStateCache(modelStateCache);
            }
            return modelStateCache.toQuads(side, rand);
        }

        ModelCollector collector = new ModelCollector(state, this.parts);
        this.modelLogic.collectModels(collector, null);
        return collector.toQuads(side, rand);
    }

    @Override
    public boolean isAmbientOcclusion() {
        return this.ambientOcclusion;
    }

    @Override
    public boolean isAmbientOcclusion(IBlockState state) {
        return this.ambientOcclusion;
    }

    @Override
    public boolean isGui3d() {
        return this.gui3d;
    }

    @Override
    public boolean isBuiltInRenderer() {
        return false;
    }

    @Override
    public TextureAtlasSprite getParticleTexture() {
        return this.particleTexture;
    }

    @Override
    public ItemOverrideList getOverrides() {
        return ItemOverrideList.NONE;
    }

    @Nonnull
    @Override
    public ModelCollector collectModels(@Nonnull ISpecialState state) {
        ModelCollector collector = new ModelCollector(state, this.parts);
        this.modelLogic.collectModels(collector, new WorldContext(state));
        return collector;
    }
}

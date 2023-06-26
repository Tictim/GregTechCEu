package gregtech.client.model.component;

import gregtech.client.utils.BloomEffectUtil;
import gregtech.common.blocks.special.ISpecialState;
import gregtech.integration.ctm.ISpecialBakedModel;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemOverrideList;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.client.MinecraftForgeClient;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;

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

    @Override
    public List<BakedQuad> getQuads(@Nullable IBlockState state, @Nullable EnumFacing side, long rand) {
        boolean includeBloomLayer;
        boolean includeNonBloomLayer;

        if (this.componentLogic.isBloomActive()) {
            includeBloomLayer = MinecraftForgeClient.getRenderLayer() == BloomEffectUtil.BLOOM;
            includeNonBloomLayer = !includeBloomLayer;
        } else {
            if (MinecraftForgeClient.getRenderLayer() == BloomEffectUtil.BLOOM) {
                return Collections.emptyList();
            }
            includeBloomLayer = MinecraftForgeClient.getRenderLayer() == BlockRenderLayer.CUTOUT;
            includeNonBloomLayer = true;
        }

        if (state instanceof ISpecialState specialState) {
            ModelCollector modelStateCache = specialState.getModelStateCache();
            if (modelStateCache == null) {
                modelStateCache = collectModels(specialState);
                specialState.setModelStateCache(modelStateCache);
            }
            return modelStateCache.toQuads(side, includeBloomLayer, includeNonBloomLayer);
        }

        ModelCollector collector = new ModelCollector(state, this.components);
        this.componentLogic.collectModels(collector, null);
        return collector.toQuads(side, includeBloomLayer, includeNonBloomLayer);
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
        ModelCollector collector = new ModelCollector(state, this.components);
        this.componentLogic.collectModels(collector, new WorldContext(state));
        return collector;
    }
}

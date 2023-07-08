package gregtech.client.model.component;

import gregtech.common.blocks.special.ISpecialState;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemOverrideList;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.client.MinecraftForgeClient;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
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

    @Nonnull
    @Override
    public List<BakedQuad> getQuads(@Nullable IBlockState state, @Nullable EnumFacing side, long rand) {
        BlockRenderLayer renderLayer = MinecraftForgeClient.getRenderLayer();
        boolean bloomActive = this.componentLogic.isBloomActive();

        ModelStates collector;

        if (state instanceof ISpecialState specialState) {
            collector = specialState.getModelStateCache().computeCache(this);
        } else {
            collector = new ModelStates(state, this.components);
            this.componentLogic.computeStates(collector, null);
        }

        return collector.toQuads(side, renderLayer, bloomActive);
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

    @Override
    public boolean isBuiltInRenderer() {
        return false;
    }

    @Nonnull
    @Override
    public TextureAtlasSprite getParticleTexture() {
        return this.particleTexture;
    }

    @Nonnull
    @Override
    public ItemOverrideList getOverrides() {
        return ItemOverrideList.NONE;
    }

    @Nonnull
    @Override
    public ModelStates collectModels(@Nonnull ISpecialState state) {
        ModelStates collector = new ModelStates(state, this.components);
        this.componentLogic.computeStates(collector, new WorldContext(state));
        return collector;
    }
}

package gregtech.client.model;

import gregtech.client.model.component.ModelStates;
import gregtech.common.blocks.special.ISpecialState;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ItemOverrideList;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.client.MinecraftForgeClient;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * Sub variation of {@link IBakedModel} with a method to provide cache key for
 * {@link team.chisel.ctm.client.model.AbstractCTMBakedModel}.
 */
public interface ISpecialBakedModel extends IBakedModel {

    @Nonnull
    ModelStates collectModels(@Nullable ISpecialState state);

    default boolean isBloomActive() {
        return true;
    }

    @Nonnull
    @Override
    default List<BakedQuad> getQuads(@Nullable IBlockState state, @Nullable EnumFacing side, long rand) {
        List<BakedQuad> quads = new ArrayList<>();
        ModelStates collector = state instanceof ISpecialState specialState ?
                specialState.getModelStateCache().computeCache(this) :
                collectModels(null);

        collector.addQuads(quads, side, MinecraftForgeClient.getRenderLayer(), rand);
        return quads;
    }

    @Override
    default boolean isBuiltInRenderer() {
        return false;
    }

    @Nonnull
    @Override
    default ItemOverrideList getOverrides() {
        return ItemOverrideList.NONE;
    }
}

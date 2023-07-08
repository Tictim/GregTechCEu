package gregtech.client.model.component;

import gregtech.common.blocks.special.ISpecialState;
import net.minecraft.client.renderer.block.model.IBakedModel;

import javax.annotation.Nonnull;

/**
 * Sub variation of {@link IBakedModel} with a method to provide cache key for
 * {@link team.chisel.ctm.client.model.AbstractCTMBakedModel}.
 */
public interface ISpecialBakedModel extends IBakedModel {

    @Nonnull
    ModelStates collectModels(@Nonnull ISpecialState state);
}

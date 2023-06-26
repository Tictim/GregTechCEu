package gregtech.integration.ctm;

import gregtech.client.model.component.ModelCollector;
import gregtech.common.blocks.special.ISpecialState;
import net.minecraft.client.renderer.block.model.IBakedModel;

import javax.annotation.Nonnull;

/**
 * Sub variation of {@link IBakedModel} with a method to provide cache key for
 * {@link team.chisel.ctm.client.model.AbstractCTMBakedModel}.
 */
public interface ISpecialBakedModel extends IBakedModel {

    @Nonnull
    ModelCollector collectModels(@Nonnull ISpecialState state);
}

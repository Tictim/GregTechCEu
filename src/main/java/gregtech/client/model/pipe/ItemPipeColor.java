package gregtech.client.model.pipe;

import gregtech.api.pipenet.tile.IPipeTile;
import gregtech.api.unification.material.properties.ItemPipeProperties;
import gregtech.common.pipelike.itempipe.BlockItemPipe;
import gregtech.common.pipelike.itempipe.ItemPipeType;
import net.minecraft.block.state.IBlockState;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;

import javax.annotation.Nonnull;

import static gregtech.client.model.pipe.PipeModelLogicProvider.*;

public class ItemPipeColor extends PipeColor<BlockItemPipe, ItemPipeType, ItemPipeProperties> {

    public ItemPipeColor(@Nonnull BlockItemPipe block) {
        super(block);
    }

    @Override
    protected int colorMultiplier(@Nonnull IBlockState state,
                                  @Nonnull IBlockAccess world,
                                  @Nonnull BlockPos pos,
                                  int tintIndex,
                                  @Nonnull IPipeTile<ItemPipeType, ItemPipeProperties> tile) {
        return switch (tintIndex) {
            case TINT_PIPE -> tile.getPaintingColor() != tile.getDefaultPaintingColor() ?
                    tile.getPaintingColor() : materialColorOrDefault(getMaterial(tile), -1);
            case TINT_FRAME, TINT_FRAME_INNER -> materialColorOrDefault(tile.getFrameMaterial(), -1);
            default -> -1;
        };
    }

    @Override
    public int colorMultiplier(@Nonnull ItemStack stack, int tintIndex) {
        return tintIndex == TINT_PIPE ? materialColorOrDefault(this.block.getItemMaterial(stack), -1) : -1;
    }
}

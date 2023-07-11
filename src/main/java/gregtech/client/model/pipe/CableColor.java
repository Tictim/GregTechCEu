package gregtech.client.model.pipe;

import gregtech.api.pipenet.tile.IPipeTile;
import gregtech.api.unification.material.properties.WireProperties;
import gregtech.common.pipelike.cable.BlockCable;
import gregtech.common.pipelike.cable.Insulation;
import net.minecraft.block.state.IBlockState;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;

import javax.annotation.Nonnull;

import static gregtech.client.model.pipe.PipeModelLogicProvider.*;

public class CableColor extends PipeColor<BlockCable, Insulation, WireProperties> {

    private static final int INSULATION_COLOR = 0x404040;

    public CableColor(@Nonnull BlockCable block) {
        super(block);
    }

    @Override
    protected int colorMultiplier(@Nonnull IBlockState state,
                                  @Nonnull IBlockAccess world,
                                  @Nonnull BlockPos pos,
                                  int tintIndex,
                                  @Nonnull IPipeTile<Insulation, WireProperties> tile) {
        return switch (tintIndex) {
            case TINT_PIPE -> tile.getPaintingColor() != tile.getDefaultPaintingColor() ?
                    tile.getPaintingColor() : materialColorOrDefault(getMaterial(tile), -1);
            case TINT_FRAME, TINT_FRAME_INNER -> materialColorOrDefault(tile.getFrameMaterial(), -1);
            case TINT_OVERLAY -> tile.getPaintingColor() != tile.getDefaultPaintingColor() ?
                    tile.getPaintingColor() : INSULATION_COLOR;
            default -> -1;
        };
    }

    @Override
    protected int fallbackColorMultiplier(@Nonnull IBlockState state, int tintIndex) {
        return tintIndex == TINT_OVERLAY ? INSULATION_COLOR : -1;
    }

    @Override
    public int colorMultiplier(@Nonnull ItemStack stack, int tintIndex) {
        return switch (tintIndex) {
            case TINT_PIPE -> materialColorOrDefault(this.block.getItemMaterial(stack), -1);
            case TINT_OVERLAY -> INSULATION_COLOR;
            default -> -1;
        };
    }
}

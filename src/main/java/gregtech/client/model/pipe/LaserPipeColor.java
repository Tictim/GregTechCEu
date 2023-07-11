package gregtech.client.model.pipe;

import gregtech.api.pipenet.tile.IPipeTile;
import gregtech.common.pipelike.laser.BlockLaserPipe;
import gregtech.common.pipelike.laser.LaserPipeProperties;
import gregtech.common.pipelike.laser.LaserPipeType;
import gregtech.common.pipelike.optical.BlockOpticalPipe;
import gregtech.common.pipelike.optical.OpticalPipeProperties;
import gregtech.common.pipelike.optical.OpticalPipeType;
import net.minecraft.block.state.IBlockState;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;

import javax.annotation.Nonnull;

import static gregtech.client.model.pipe.PipeModelLogicProvider.TINT_OVERLAY;

public class LaserPipeColor extends PipeColor<BlockLaserPipe, LaserPipeType, LaserPipeProperties> {

    public LaserPipeColor(@Nonnull BlockLaserPipe block) {
        super(block);
    }

    @Override
    public int colorMultiplier(@Nonnull ItemStack stack, int tintIndex) {
        return -1;
    }

    @Override
    protected int colorMultiplier(@Nonnull IBlockState state,
                                  @Nonnull IBlockAccess world,
                                  @Nonnull BlockPos pos,
                                  int tintIndex,
                                  @Nonnull IPipeTile<LaserPipeType, LaserPipeProperties> tile) {
        return tintIndex == TINT_OVERLAY ? tile.getPaintingColor() : -1;
    }
}

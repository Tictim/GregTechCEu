package gregtech.common.blocks.extendedstate;

import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;

import javax.annotation.Nonnull;

/**
 * Override to Block class to provide custom connection check algo
 */
public interface ConnectionChecker {

    boolean isConnectedToNeighbor(@Nonnull IBlockAccess world, @Nonnull IBlockState originState, @Nonnull BlockPos origin, @Nonnull BlockPos neighbor);
}

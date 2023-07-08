package gregtech.common.blocks.special;

import gregtech.client.utils.ModelStateCache;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraftforge.common.property.IExtendedBlockState;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nonnull;

public interface ISpecialState extends IExtendedBlockState {

    @Nonnull
    IBlockAccess getWorld();

    @Nonnull
    BlockPos getPos();

    @Nonnull
    @SideOnly(Side.CLIENT)
    ModelStateCache getModelStateCache();
}

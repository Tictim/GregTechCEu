package gregtech.common.blocks.special;

import gregtech.client.model.special.ModelCollector;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraftforge.common.property.IExtendedBlockState;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public interface ISpecialState extends IExtendedBlockState {

    @Nonnull
    IBlockAccess getWorld();

    @Nonnull
    BlockPos getPos();

    @Nullable
    @SideOnly(Side.CLIENT)
    ModelCollector getModelStateCache();

    @SideOnly(Side.CLIENT)
    void setModelStateCache(@Nullable ModelCollector modelStateCache);
}

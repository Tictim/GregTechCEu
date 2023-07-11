package gregtech.client.model.pipe;

import gregtech.api.pipenet.block.BlockPipe;
import gregtech.api.pipenet.block.IPipeType;
import gregtech.api.pipenet.block.material.TileEntityMaterialPipeBase;
import gregtech.api.pipenet.tile.IPipeTile;
import gregtech.api.unification.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.color.BlockColors;
import net.minecraft.client.renderer.color.IBlockColor;
import net.minecraft.client.renderer.color.IItemColor;
import net.minecraft.client.renderer.color.ItemColors;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Objects;

public abstract class PipeColor<
        T extends BlockPipe<PipeType, NodeDataType, ?>,
        PipeType extends Enum<PipeType> & IPipeType<NodeDataType>,
        NodeDataType
        > implements IBlockColor, IItemColor {

    protected final T block;

    public PipeColor(@Nonnull T block) {
        this.block = Objects.requireNonNull(block, "block == null");
    }

    public void register(@Nonnull BlockColors blockColors, @Nonnull ItemColors itemColors){
        blockColors.registerBlockColorHandler(this, this.block);
        itemColors.registerItemColorHandler(this, this.block);
    }

    @Override
    public int colorMultiplier(@Nonnull IBlockState state,
                               @Nullable IBlockAccess world,
                               @Nullable BlockPos pos,
                               int tintIndex) {
        if (world != null && pos != null) {
            IPipeTile<PipeType, NodeDataType> te = this.block.getPipeTileEntity(world, pos);
            if (te != null) return colorMultiplier(state, world, pos, tintIndex, te);
        }
        return fallbackColorMultiplier(state, tintIndex);
    }

    protected abstract int colorMultiplier(@Nonnull IBlockState state,
                                           @Nonnull IBlockAccess world,
                                           @Nonnull BlockPos pos,
                                           int tintIndex,
                                           @Nonnull IPipeTile<PipeType, NodeDataType> tile);

    protected int fallbackColorMultiplier(@Nonnull IBlockState state, int tintIndex) {
        return -1;
    }

    @Nullable
    protected static Material getMaterial(@Nonnull IPipeTile<?, ?> pipeTile) {
        return pipeTile instanceof TileEntityMaterialPipeBase te ? te.getPipeMaterial() : null;
    }

    protected static int materialColorOrDefault(@Nullable Material material, int defaultColor) {
        return material != null ? material.getMaterialRGB() : defaultColor;
    }
}

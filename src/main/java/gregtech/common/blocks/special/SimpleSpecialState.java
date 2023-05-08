package gregtech.common.blocks.special;

import com.google.common.collect.ImmutableMap;
import net.minecraft.block.Block;
import net.minecraft.block.material.EnumPushReaction;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.BlockStateBase;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.EnumBlockRenderType;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.Mirror;
import net.minecraft.util.Rotation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.common.property.IExtendedBlockState;
import net.minecraftforge.common.property.IUnlistedProperty;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class SimpleSpecialState extends BlockStateBase implements ISpecialState {

    @Nonnull
    private final IBlockState delegate;

    @Nullable
    private final IExtendedBlockState extState;

    @Nonnull
    private final IBlockAccess world;
    @Nonnull
    private final BlockPos pos;

    public SimpleSpecialState(@Nonnull IBlockState delegate, @Nonnull IBlockAccess world, @Nonnull BlockPos pos) {
        this.delegate = delegate;
        this.extState = delegate instanceof IExtendedBlockState ? (IExtendedBlockState) delegate : null;
        this.world = world;
        this.pos = pos;
    }

    @Nonnull
    @Override
    public IBlockAccess getWorld() {
        return world;
    }

    @Nonnull
    @Override
    public BlockPos getPos() {
        return pos;
    }

    @Nullable
    @Override
    public Collection<IUnlistedProperty<?>> getUnlistedNames() {
        return this.extState != null ? this.extState.getUnlistedNames() : Collections.emptyList();
    }

    @Nullable
    @Override
    public <V> V getValue(@Nullable IUnlistedProperty<V> property) {
        return this.extState != null ? this.extState.getValue(property) : null;
    }

    @Nullable
    @Override
    public <V> IExtendedBlockState withProperty(@Nullable IUnlistedProperty<V> property, @Nullable V value) {
        return this.extState != null ? new SimpleSpecialState(this.extState.withProperty(property, value), this.world, this.pos) : this;
    }

    @Nullable
    @Override
    public ImmutableMap<IUnlistedProperty<?>, Optional<?>> getUnlistedProperties() {
        return this.extState != null ? this.extState.getUnlistedProperties() : ImmutableMap.of();
    }

    @Nonnull
    @Override
    public IBlockState getClean() {
        return this.extState != null ? this.extState.getClean() : this.delegate;
    }

    @Override
    public <T extends Comparable<T>, V extends T> IBlockState withProperty(IProperty<T> property, V value) {
        return new SimpleSpecialState(delegate.withProperty(property, value), this.world, this.pos);
    }

    @Override
    public <T extends Comparable<T>> IBlockState cycleProperty(IProperty<T> property) {
        return new SimpleSpecialState(delegate.cycleProperty(property), this.world, this.pos);
    }

    /*
     * Delegated methods below
     */

    @Override
    public Collection<IProperty<?>> getPropertyKeys() {return delegate.getPropertyKeys();}

    @Override
    public <T extends Comparable<T>> T getValue(IProperty<T> property) {
        return delegate.getValue(property);
    }

    @Override
    public ImmutableMap<IProperty<?>, Comparable<?>> getProperties() {return delegate.getProperties();}

    @Override
    public Block getBlock() {return delegate.getBlock();}

    @Override
    public boolean onBlockEventReceived(World worldIn, BlockPos pos, int id, int param) {return delegate.onBlockEventReceived(worldIn, pos, id, param);}

    @Override
    public void neighborChanged(World worldIn, BlockPos pos, Block blockIn, BlockPos fromPos) {delegate.neighborChanged(worldIn, pos, blockIn, fromPos);}

    @Override
    public Material getMaterial() {return delegate.getMaterial();}

    @Override
    public boolean isFullBlock() {return delegate.isFullBlock();}

    @Override
    public boolean canEntitySpawn(Entity entityIn) {return delegate.canEntitySpawn(entityIn);}

    @Override
    @Deprecated
    public int getLightOpacity() {return delegate.getLightOpacity();}

    @Override
    public int getLightOpacity(IBlockAccess world, BlockPos pos) {return delegate.getLightOpacity(world, pos);}

    @Override
    @Deprecated
    public int getLightValue() {return delegate.getLightValue();}

    @Override
    public int getLightValue(IBlockAccess world, BlockPos pos) {return delegate.getLightValue(world, pos);}

    @Override
    @SideOnly(Side.CLIENT)
    public boolean isTranslucent() {return delegate.isTranslucent();}

    @Override
    public boolean useNeighborBrightness() {return delegate.useNeighborBrightness();}

    @Override
    public MapColor getMapColor(IBlockAccess p_185909_1_, BlockPos p_185909_2_) {return delegate.getMapColor(p_185909_1_, p_185909_2_);}

    @Override
    public IBlockState withRotation(Rotation rot) {return delegate.withRotation(rot);}

    @Override
    public IBlockState withMirror(Mirror mirrorIn) {return delegate.withMirror(mirrorIn);}

    @Override
    public boolean isFullCube() {return delegate.isFullCube();}

    @Override
    @SideOnly(Side.CLIENT)
    public boolean hasCustomBreakingProgress() {return delegate.hasCustomBreakingProgress();}

    @Override
    public EnumBlockRenderType getRenderType() {return delegate.getRenderType();}

    @Override
    @SideOnly(Side.CLIENT)
    public int getPackedLightmapCoords(IBlockAccess source, BlockPos pos) {return delegate.getPackedLightmapCoords(source, pos);}

    @Override
    @SideOnly(Side.CLIENT)
    public float getAmbientOcclusionLightValue() {return delegate.getAmbientOcclusionLightValue();}

    @Override
    public boolean isBlockNormalCube() {return delegate.isBlockNormalCube();}

    @Override
    public boolean isNormalCube() {return delegate.isNormalCube();}

    @Override
    public boolean canProvidePower() {return delegate.canProvidePower();}

    @Override
    public int getWeakPower(IBlockAccess blockAccess, BlockPos pos, EnumFacing side) {return delegate.getWeakPower(blockAccess, pos, side);}

    @Override
    public boolean hasComparatorInputOverride() {return delegate.hasComparatorInputOverride();}

    @Override
    public int getComparatorInputOverride(World worldIn, BlockPos pos) {return delegate.getComparatorInputOverride(worldIn, pos);}

    @Override
    public float getBlockHardness(World worldIn, BlockPos pos) {return delegate.getBlockHardness(worldIn, pos);}

    @Override
    public float getPlayerRelativeBlockHardness(EntityPlayer player, World worldIn, BlockPos pos) {return delegate.getPlayerRelativeBlockHardness(player, worldIn, pos);}

    @Override
    public int getStrongPower(IBlockAccess blockAccess, BlockPos pos, EnumFacing side) {return delegate.getStrongPower(blockAccess, pos, side);}

    @Override
    public EnumPushReaction getPushReaction() {return delegate.getPushReaction();}

    @Override
    public IBlockState getActualState(IBlockAccess blockAccess, BlockPos pos) {return delegate.getActualState(blockAccess, pos);}

    @Override
    @SideOnly(Side.CLIENT)
    public AxisAlignedBB getSelectedBoundingBox(World worldIn, BlockPos pos) {return delegate.getSelectedBoundingBox(worldIn, pos);}

    @Override
    @SideOnly(Side.CLIENT)
    public boolean shouldSideBeRendered(IBlockAccess blockAccess, BlockPos pos, EnumFacing facing) {return delegate.shouldSideBeRendered(blockAccess, pos, facing);}

    @Override
    public boolean isOpaqueCube() {return delegate.isOpaqueCube();}

    @Override
    @Nullable
    public AxisAlignedBB getCollisionBoundingBox(IBlockAccess worldIn, BlockPos pos) {return delegate.getCollisionBoundingBox(worldIn, pos);}

    @Override
    public void addCollisionBoxToList(World worldIn, BlockPos pos, AxisAlignedBB entityBox, List<AxisAlignedBB> collidingBoxes, @Nullable Entity entityIn, boolean p_185908_6_) {delegate.addCollisionBoxToList(worldIn, pos, entityBox, collidingBoxes, entityIn, p_185908_6_);}

    @Override
    public AxisAlignedBB getBoundingBox(IBlockAccess blockAccess, BlockPos pos) {return delegate.getBoundingBox(blockAccess, pos);}

    @Override
    public RayTraceResult collisionRayTrace(World worldIn, BlockPos pos, Vec3d start, Vec3d end) {return delegate.collisionRayTrace(worldIn, pos, start, end);}

    @Override
    @Deprecated
    public boolean isTopSolid() {return delegate.isTopSolid();}

    @Override
    public boolean doesSideBlockRendering(IBlockAccess world, BlockPos pos, EnumFacing side) {return delegate.doesSideBlockRendering(world, pos, side);}

    @Override
    public boolean isSideSolid(IBlockAccess world, BlockPos pos, EnumFacing side) {return delegate.isSideSolid(world, pos, side);}

    @Override
    public boolean doesSideBlockChestOpening(IBlockAccess world, BlockPos pos, EnumFacing side) {return delegate.doesSideBlockChestOpening(world, pos, side);}

    @Override
    public Vec3d getOffset(IBlockAccess access, BlockPos pos) {return delegate.getOffset(access, pos);}

    @Override
    public boolean causesSuffocation() {return delegate.causesSuffocation();}

    @Override
    public BlockFaceShape getBlockFaceShape(IBlockAccess worldIn, BlockPos pos, EnumFacing facing) {return delegate.getBlockFaceShape(worldIn, pos, facing);}
}

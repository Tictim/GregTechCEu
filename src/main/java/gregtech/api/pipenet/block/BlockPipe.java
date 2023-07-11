package gregtech.api.pipenet.block;

import codechicken.lib.raytracer.CuboidRayTraceResult;
import codechicken.lib.raytracer.IndexedCuboid6;
import codechicken.lib.raytracer.RayTracer;
import codechicken.lib.texture.TextureUtils;
import codechicken.lib.vec.Cuboid6;
import gregtech.api.GTValues;
import gregtech.api.block.BlockCustomParticle;
import gregtech.api.cover.CoverBehavior;
import gregtech.api.cover.ICoverable;
import gregtech.api.cover.ICoverable.CoverSideData;
import gregtech.api.cover.ICoverable.PrimaryBoxData;
import gregtech.api.cover.IFacadeCover;
import gregtech.api.items.toolitem.ToolClasses;
import gregtech.api.items.toolitem.ToolHelper;
import gregtech.api.pipenet.IBlockAppearance;
import gregtech.api.pipenet.PipeNet;
import gregtech.api.pipenet.WorldPipeNet;
import gregtech.api.pipenet.tile.IPipeTile;
import gregtech.api.pipenet.tile.PipeCoverableImplementation;
import gregtech.api.pipenet.tile.TileEntityPipeBase;
import gregtech.api.unification.material.Material;
import gregtech.api.util.GTUtility;
import gregtech.common.ConfigHolder;
import gregtech.common.blocks.BlockFrame;
import gregtech.common.blocks.MetaBlocks;
import gregtech.common.blocks.special.CTMSpecialState;
import gregtech.common.blocks.special.SimpleSpecialState;
import gregtech.common.items.MetaItems;
import gregtech.integration.ctm.IFacadeWrapper;
import net.minecraft.block.Block;
import net.minecraft.block.ITileEntityProvider;
import net.minecraft.block.SoundType;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.*;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.apache.commons.lang3.tuple.Pair;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import static gregtech.api.metatileentity.MetaTileEntity.FULL_CUBE_COLLISION;

@SuppressWarnings("deprecation")
public abstract class BlockPipe<PipeType extends Enum<PipeType> & IPipeType<NodeDataType>, NodeDataType, WorldPipeNetType extends WorldPipeNet<NodeDataType, ? extends PipeNet<NodeDataType>>> extends BlockCustomParticle implements ITileEntityProvider, IFacadeWrapper, IBlockAppearance {

    protected final ThreadLocal<IPipeTile<PipeType, NodeDataType>> tileEntities = new ThreadLocal<>();

    public BlockPipe() {
        super(net.minecraft.block.material.Material.IRON);
        setTranslationKey("pipe");
        setSoundType(SoundType.METAL);
        setHardness(2.0f);
        setResistance(3.0f);
        setLightOpacity(0);
        disableStats();
    }

    /**
     * Get cuboid shape for pipe section's area.
     *
     * @param side      The specific section of the pipe; more specifically,
     *                  <ul>
     *                  <li>{@code null} represents the central cube part of the pipe.</li>
     *                  <li>Any nonnull values represent the branch part of the pipe, corresponding to the direction.</li>
     *                  </ul>
     * @param thickness Thickness of the pipe
     * @return Cuboid shape for pipe section's area
     */
    public static Cuboid6 getSideBox(@Nullable EnumFacing side, float thickness) {
        float min = (1.0f - thickness) / 2.0f, max = min + thickness;
        float faceMin = 0f, faceMax = 1f;

        return side == null ? new Cuboid6(min, min, min, max, max, max) :
                switch (side) {
                    case WEST -> new Cuboid6(faceMin, min, min, min, max, max);
                    case EAST -> new Cuboid6(max, min, min, faceMax, max, max);
                    case NORTH -> new Cuboid6(min, min, faceMin, max, max, min);
                    case SOUTH -> new Cuboid6(min, min, max, max, max, faceMax);
                    case UP -> new Cuboid6(min, max, min, max, faceMax, max);
                    case DOWN -> new Cuboid6(min, faceMin, min, max, min, max);
                };
    }

    public abstract Class<PipeType> getPipeTypeClass();

    public abstract WorldPipeNetType getWorldPipeNet(World world);

    public abstract TileEntityPipeBase<PipeType, NodeDataType> createNewTileEntity(boolean supportsTicking);

    public abstract NodeDataType createProperties(IPipeTile<PipeType, NodeDataType> pipeTile);

    public abstract NodeDataType createItemProperties(ItemStack itemStack);

    public abstract ItemStack getDropItem(IPipeTile<PipeType, NodeDataType> pipeTile);

    protected abstract NodeDataType getFallbackType();

    // TODO this has no reason to need an ItemStack parameter
    public abstract PipeType getItemPipeType(ItemStack itemStack);

    public abstract void setTileEntityData(TileEntityPipeBase<PipeType, NodeDataType> pipeTile, ItemStack itemStack);

    @Override
    public abstract void getSubBlocks(@Nonnull CreativeTabs itemIn, @Nonnull NonNullList<ItemStack> items);

    @Override
    public void breakBlock(@Nonnull World worldIn, @Nonnull BlockPos pos, @Nonnull IBlockState state) {
        IPipeTile<PipeType, NodeDataType> pipeTile = getPipeTileEntity(worldIn, pos);
        if (pipeTile != null) {
            pipeTile.getCoverableImplementation().dropAllCovers();
            tileEntities.set(pipeTile);
        }
        super.breakBlock(worldIn, pos, state);
        getWorldPipeNet(worldIn).removeNode(pos);
    }

    @Override
    public void onBlockAdded(World worldIn, @Nonnull BlockPos pos, @Nonnull IBlockState state) {
        worldIn.scheduleUpdate(pos, this, 1);
    }

    @Override
    public void updateTick(@Nonnull World worldIn, @Nonnull BlockPos pos, @Nonnull IBlockState state, @Nonnull Random rand) {
        IPipeTile<PipeType, NodeDataType> pipeTile = getPipeTileEntity(worldIn, pos);
        if (pipeTile != null) {
            int activeConnections = pipeTile.getConnections();
            boolean isActiveNode = activeConnections != 0;
            getWorldPipeNet(worldIn).addNode(pos, createProperties(pipeTile), 0, activeConnections, isActiveNode);
            onActiveModeChange(worldIn, pos, isActiveNode, true);
        }
    }

    @Override
    public void onBlockPlacedBy(@Nonnull World worldIn, @Nonnull BlockPos pos, @Nonnull IBlockState state, @Nonnull EntityLivingBase placer, @Nonnull ItemStack stack) {
        IPipeTile<PipeType, NodeDataType> pipeTile = getPipeTileEntity(worldIn, pos);
        if (pipeTile != null) {
            setTileEntityData((TileEntityPipeBase<PipeType, NodeDataType>) pipeTile, stack);

            // Color pipes/cables on place if holding spray can in off-hand
            if (placer instanceof EntityPlayer player) {
                ItemStack offhand = placer.getHeldItemOffhand();
                for (int i = 0; i < EnumDyeColor.values().length; i++) {
                    if (offhand.isItemEqual(MetaItems.SPRAY_CAN_DYES[i].getStackForm())) {
                        MetaItems.SPRAY_CAN_DYES[i].getBehaviours().get(0).onItemUse(player, worldIn, pos, EnumHand.OFF_HAND, EnumFacing.UP, 0, 0, 0);
                        break;
                    }
                }
            }
        }
    }

    @Override
    public void neighborChanged(@Nonnull IBlockState state, @Nonnull World worldIn, @Nonnull BlockPos pos, @Nonnull Block blockIn, @Nonnull BlockPos fromPos) {
        if (worldIn.isRemote) return;
        IPipeTile<PipeType, NodeDataType> pipeTile = getPipeTileEntity(worldIn, pos);
        if (pipeTile != null) {
            pipeTile.getCoverableImplementation().updateInputRedstoneSignals();
            if (!ConfigHolder.machines.gt6StylePipesCables) {
                EnumFacing facing = null;
                for (EnumFacing facing1 : EnumFacing.values()) {
                    if (GTUtility.arePosEqual(fromPos, pos.offset(facing1))) {
                        facing = facing1;
                        break;
                    }
                }
                if (facing == null) {
                    //not our neighbor
                    return;
                }
                boolean open = pipeTile.isConnected(facing);
                boolean canConnect = pipeTile.getCoverableImplementation().getCoverAtSide(facing) != null || canConnect(pipeTile, facing);
                if (!open && canConnect && state.getBlock() != blockIn)
                    pipeTile.setConnection(facing, true, false);
                if (open && !canConnect)
                    pipeTile.setConnection(facing, false, false);
                updateActiveNodeStatus(worldIn, pos, pipeTile);
            }
        }
    }

    @Override
    public void observedNeighborChange(@Nonnull IBlockState observerState, @Nonnull World world, @Nonnull BlockPos observerPos, @Nonnull Block changedBlock, @Nonnull BlockPos changedBlockPos) {
        PipeNet<NodeDataType> net = getWorldPipeNet(world).getNetFromPos(observerPos);
        if (net != null) {
            net.onNeighbourUpdate(changedBlockPos);
        }
    }

    @Override
    public boolean canConnectRedstone(@Nonnull IBlockState state, @Nonnull IBlockAccess world, @Nonnull BlockPos pos, @Nullable EnumFacing side) {
        IPipeTile<PipeType, NodeDataType> pipeTile = getPipeTileEntity(world, pos);
        return pipeTile != null && pipeTile.getCoverableImplementation().canConnectRedstone(side);
    }

    @Override
    public boolean shouldCheckWeakPower(@Nonnull IBlockState state, @Nonnull IBlockAccess world, @Nonnull BlockPos pos, @Nonnull EnumFacing side) {
        // The check in World::getRedstonePower in the vanilla code base is reversed. Setting this to false will
        // actually cause getWeakPower to be called, rather than prevent it.
        return false;
    }

    @Override
    public int getWeakPower(@Nonnull IBlockState blockState, @Nonnull IBlockAccess blockAccess, @Nonnull BlockPos pos, @Nonnull EnumFacing side) {
        IPipeTile<PipeType, NodeDataType> pipeTile = getPipeTileEntity(blockAccess, pos);
        return pipeTile == null ? 0 : pipeTile.getCoverableImplementation().getOutputRedstoneSignal(side.getOpposite());
    }

    public void updateActiveNodeStatus(World worldIn, BlockPos pos, IPipeTile<PipeType, NodeDataType> pipeTile) {
        PipeNet<NodeDataType> pipeNet = getWorldPipeNet(worldIn).getNetFromPos(pos);
        if (pipeNet != null && pipeTile != null) {
            int activeConnections = pipeTile.getConnections(); //remove blocked connections
            boolean isActiveNodeNow = activeConnections != 0;
            boolean modeChanged = pipeNet.markNodeAsActive(pos, isActiveNodeNow);
            if (modeChanged) {
                onActiveModeChange(worldIn, pos, isActiveNodeNow, false);
            }
        }
    }

    @Nullable
    @Override
    public TileEntity createNewTileEntity(@Nonnull World worldIn, int meta) {
        return createNewTileEntity(false);
    }

    /**
     * Can be used to update tile entity to tickable when node becomes active
     * usable for fluid pipes, as example
     */
    protected void onActiveModeChange(World world, BlockPos pos, boolean isActiveNow, boolean isInitialChange) {}

    @Nonnull
    @Override
    public ItemStack getPickBlock(@Nonnull IBlockState state, @Nonnull RayTraceResult target, @Nonnull World world, @Nonnull BlockPos pos, @Nonnull EntityPlayer player) {
        IPipeTile<PipeType, NodeDataType> pipeTile = getPipeTileEntity(world, pos);
        if (pipeTile == null) {
            return ItemStack.EMPTY;
        }
        if (target instanceof CuboidRayTraceResult result && result.cuboid6.data instanceof CoverSideData coverSideData) {
            CoverBehavior coverBehavior = pipeTile.getCoverableImplementation().getCoverAtSide(coverSideData.side);
            return coverBehavior == null ? ItemStack.EMPTY : coverBehavior.getPickItem();
        }
        return getDropItem(pipeTile);
    }

    @Override
    public boolean onBlockActivated(@Nonnull World worldIn, @Nonnull BlockPos pos, @Nonnull IBlockState state, @Nonnull EntityPlayer playerIn, @Nonnull EnumHand hand, @Nonnull EnumFacing facing, float hitX, float hitY, float hitZ) {
        IPipeTile<PipeType, NodeDataType> pipeTile = getPipeTileEntity(worldIn, pos);
        CuboidRayTraceResult rayTraceResult = (CuboidRayTraceResult) RayTracer.retraceBlock(worldIn, playerIn, pos);
        if (rayTraceResult == null || pipeTile == null) {
            return false;
        }
        return onPipeActivated(worldIn, state, pos, playerIn, hand, facing, rayTraceResult, pipeTile);
    }

    public boolean onPipeActivated(World world, IBlockState state, BlockPos pos, EntityPlayer entityPlayer, EnumHand hand, EnumFacing side, CuboidRayTraceResult hit, IPipeTile<PipeType, NodeDataType> pipeTile) {
        ItemStack itemStack = entityPlayer.getHeldItem(hand);

        if (pipeTile.getFrameMaterial() == null &&
                pipeTile instanceof TileEntityPipeBase pipeBase &&
                pipeTile.getPipeType().getThickness() < 1) {
            BlockFrame frameBlock = BlockFrame.getFrameBlockFromItem(itemStack);
            if (frameBlock != null) {
                pipeBase.setFrameMaterial(frameBlock.getGtMaterial(itemStack));
                SoundType type = frameBlock.getSoundType(itemStack);
                world.playSound(entityPlayer, pos, type.getPlaceSound(), SoundCategory.BLOCKS, (type.getVolume() + 1.0F) / 2.0F, type.getPitch() * 0.8F);
                if (!entityPlayer.capabilities.isCreativeMode) {
                    itemStack.shrink(1);
                }
                return true;
            }
        }

        if (itemStack.getItem() instanceof ItemBlockPipe itemBlockPipe) {
            IBlockState blockStateAtSide = world.getBlockState(pos.offset(side));
            if (blockStateAtSide.getBlock() instanceof BlockFrame blockFrame &&
                    itemBlockPipe.blockPipe.getItemPipeType(itemStack) == getItemPipeType(itemStack)) {
                boolean wasPlaced = blockFrame.replaceWithFramedPipe(world, pos.offset(side), blockStateAtSide, entityPlayer, itemStack, side);
                if (wasPlaced) {
                    pipeTile.setConnection(side, true, false);
                }
                return wasPlaced;
            }
        }

        EnumFacing coverSide = ICoverable.traceCoverSide(hit);
        if (coverSide == null) {
            return activateFrame(world, state, pos, entityPlayer, hand, hit, pipeTile);
        }

        if (!(hit.cuboid6.data instanceof CoverSideData)) {
            switch (onPipeToolUsed(world, pos, itemStack, coverSide, pipeTile, entityPlayer, hand)) {
                case SUCCESS:
                    return true;
                case FAIL:
                    return false;
            }
        }

        CoverBehavior coverBehavior = pipeTile.getCoverableImplementation().getCoverAtSide(coverSide);
        if (coverBehavior == null) {
            return activateFrame(world, state, pos, entityPlayer, hand, hit, pipeTile);
        }

        if (itemStack.getItem().getToolClasses(itemStack).contains(ToolClasses.SOFT_MALLET)) {
            if (coverBehavior.onSoftMalletClick(entityPlayer, hand, hit) == EnumActionResult.SUCCESS) {
                ToolHelper.damageItem(itemStack, entityPlayer);
                ToolHelper.playToolSound(itemStack, entityPlayer);
                return true;
            }
        }

        if (itemStack.getItem().getToolClasses(itemStack).contains(ToolClasses.SCREWDRIVER)) {
            if (coverBehavior.onScrewdriverClick(entityPlayer, hand, hit) == EnumActionResult.SUCCESS) {
                ToolHelper.damageItem(itemStack, entityPlayer);
                ToolHelper.playToolSound(itemStack, entityPlayer);
                return true;
            }
        }

        if (itemStack.getItem().getToolClasses(itemStack).contains(ToolClasses.CROWBAR)) {
            if (!world.isRemote) {
                if (pipeTile.getCoverableImplementation().removeCover(coverSide)) {
                    ToolHelper.damageItem(itemStack, entityPlayer);
                    ToolHelper.playToolSound(itemStack, entityPlayer);
                    return true;
                }
            }
        }

        EnumActionResult result = coverBehavior.onRightClick(entityPlayer, hand, hit);
        if (result == EnumActionResult.PASS) {
            if (activateFrame(world, state, pos, entityPlayer, hand, hit, pipeTile)) {
                return true;
            }
            return entityPlayer.isSneaking() && entityPlayer.getHeldItemMainhand().isEmpty() && coverBehavior.onScrewdriverClick(entityPlayer, hand, hit) != EnumActionResult.PASS;
        }
        return true;
    }

    private boolean activateFrame(World world, IBlockState state, BlockPos pos, EntityPlayer entityPlayer, EnumHand hand, CuboidRayTraceResult hit, IPipeTile<PipeType, NodeDataType> pipeTile) {
        if (pipeTile.getFrameMaterial() != null && !(entityPlayer.getHeldItem(hand).getItem() instanceof ItemBlockPipe)) {
            BlockFrame blockFrame = MetaBlocks.FRAMES.get(pipeTile.getFrameMaterial());
            return blockFrame.onBlockActivated(world, pos, state, entityPlayer, hand, hit.sideHit, (float) hit.hitVec.x, (float) hit.hitVec.y, (float) hit.hitVec.z);
        }
        return false;
    }

    /**
     * @return 1 if successfully used tool, 0 if failed to use tool,
     * -1 if ItemStack failed the capability check (no action done, continue checks).
     */
    public EnumActionResult onPipeToolUsed(World world, BlockPos pos, ItemStack stack, EnumFacing coverSide, IPipeTile<PipeType, NodeDataType> pipeTile, EntityPlayer entityPlayer, EnumHand hand) {
        if (isPipeTool(stack)) {
            if (!entityPlayer.world.isRemote) {
                if (entityPlayer.isSneaking() && pipeTile.canHaveBlockedFaces()) {
                    boolean isBlocked = pipeTile.isFaceBlocked(coverSide);
                    pipeTile.setFaceBlocked(coverSide, !isBlocked);
                    ToolHelper.playToolSound(stack, entityPlayer);
                } else {
                    boolean isOpen = pipeTile.isConnected(coverSide);
                    pipeTile.setConnection(coverSide, !isOpen, false);
                    if (isOpen != pipeTile.isConnected(coverSide)) {
                        ToolHelper.playToolSound(stack, entityPlayer);
                    }
                }
                ToolHelper.damageItem(stack, entityPlayer);
                return EnumActionResult.SUCCESS;
            }
            entityPlayer.swingArm(hand);
        }
        return EnumActionResult.PASS;
    }

    protected boolean isPipeTool(@Nonnull ItemStack stack) {
        return ToolHelper.isTool(stack, ToolClasses.WRENCH);
    }

    @Override
    public void onBlockClicked(@Nonnull World worldIn, @Nonnull BlockPos pos, @Nonnull EntityPlayer playerIn) {
        IPipeTile<PipeType, NodeDataType> pipeTile = getPipeTileEntity(worldIn, pos);
        CuboidRayTraceResult rayTraceResult = (CuboidRayTraceResult) RayTracer.retraceBlock(worldIn, playerIn, pos);
        if (pipeTile == null || rayTraceResult == null) {
            return;
        }
        EnumFacing coverSide = ICoverable.traceCoverSide(rayTraceResult);
        CoverBehavior coverBehavior = coverSide == null ? null : pipeTile.getCoverableImplementation().getCoverAtSide(coverSide);

        if (coverBehavior != null) {
            coverBehavior.onLeftClick(playerIn, rayTraceResult);
        }
    }

    @Override
    public void onEntityCollision(@Nonnull World worldIn, @Nonnull BlockPos pos, @Nonnull IBlockState state, @Nonnull Entity entityIn) {
        IPipeTile<PipeType, NodeDataType> pipeTile = getPipeTileEntity(worldIn, pos);
        if (pipeTile != null && pipeTile.getFrameMaterial() != null) {
            // make pipe with frame climbable
            BlockFrame blockFrame = MetaBlocks.FRAMES.get(pipeTile.getFrameMaterial());
            blockFrame.onEntityCollision(worldIn, pos, state, entityIn);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public void harvestBlock(@Nonnull World worldIn, @Nonnull EntityPlayer player, @Nonnull BlockPos pos, @Nonnull IBlockState state, @Nullable TileEntity te, @Nonnull ItemStack stack) {
        tileEntities.set(te == null ? tileEntities.get() : (IPipeTile<PipeType, NodeDataType>) te);
        super.harvestBlock(worldIn, player, pos, state, te, stack);
        tileEntities.set(null);
    }

    @Override
    public void getDrops(@Nonnull NonNullList<ItemStack> drops, @Nonnull IBlockAccess world, @Nonnull BlockPos pos, @Nonnull IBlockState state, int fortune) {
        IPipeTile<PipeType, NodeDataType> pipeTile = tileEntities.get() == null ? getPipeTileEntity(world, pos) : tileEntities.get();
        if (pipeTile == null) return;
        if (pipeTile.getFrameMaterial() != null) {
            BlockFrame blockFrame = MetaBlocks.FRAMES.get(pipeTile.getFrameMaterial());
            drops.add(blockFrame.getItem(pipeTile.getFrameMaterial()));
        }
        drops.add(getDropItem(pipeTile));
    }

    @Override
    public void addCollisionBoxToList(@Nonnull IBlockState state, @Nonnull World worldIn, @Nonnull BlockPos pos, @Nonnull AxisAlignedBB entityBox, @Nonnull List<AxisAlignedBB> collidingBoxes, @Nullable Entity entityIn, boolean isActualState) {
        // This iterator causes some heap memory overhead
        IPipeTile<PipeType, NodeDataType> pipeTile = getPipeTileEntity(worldIn, pos);
        if (pipeTile != null && pipeTile.getFrameMaterial() != null) {
            PipeCoverableImplementation coverable = pipeTile.getCoverableImplementation();

            int coverFlags = 0;
            for (EnumFacing side : EnumFacing.HORIZONTALS) {
                if (coverable.getCoverAtSide(side) == null)
                    coverFlags |= 1 << side.getHorizontalIndex(); // only sides without any cover should be climbable
            }

            AxisAlignedBB box = BlockFrame.getCollisionBox(coverFlags).offset(pos);
            if (box.intersects(entityBox)) {
                collidingBoxes.add(box);
            }
            return;
        }
        for (Cuboid6 axisAlignedBB : getCollisionBox(worldIn, pos, entityIn)) {
            AxisAlignedBB offsetBox = axisAlignedBB.aabb().offset(pos);
            if (offsetBox.intersects(entityBox)) collidingBoxes.add(offsetBox);
        }
    }

    @Nullable
    @Override
    public RayTraceResult collisionRayTrace(@Nonnull IBlockState blockState, World worldIn, @Nonnull BlockPos pos, @Nonnull Vec3d start, @Nonnull Vec3d end) {
        if (worldIn.isRemote) {
            return getClientCollisionRayTrace(worldIn, pos, start, end);
        }
        return RayTracer.rayTraceCuboidsClosest(start, end, pos, FULL_CUBE_COLLISION);
    }

    @SideOnly(Side.CLIENT)
    public RayTraceResult getClientCollisionRayTrace(World worldIn, @Nonnull BlockPos pos, @Nonnull Vec3d start, @Nonnull Vec3d end) {
        return RayTracer.rayTraceCuboidsClosest(start, end, pos, getCollisionBox(worldIn, pos, Minecraft.getMinecraft().player));
    }

    @Nonnull
    @Override
    public BlockFaceShape getBlockFaceShape(@Nonnull IBlockAccess worldIn, @Nonnull IBlockState state, @Nonnull BlockPos pos, @Nonnull EnumFacing face) {
        return BlockFaceShape.UNDEFINED;
    }

    @Override
    public boolean recolorBlock(@Nonnull World world, @Nonnull BlockPos pos, @Nonnull EnumFacing side, @Nonnull EnumDyeColor color) {
        IPipeTile<PipeType, NodeDataType> tileEntityPipe = getPipeTileEntity(world, pos);
        if (tileEntityPipe != null && tileEntityPipe.getPipeType() != null &&
                tileEntityPipe.getPipeType().isPaintable() &&
                tileEntityPipe.getPaintingColor() != color.colorValue) {
            tileEntityPipe.setPaintingColor(color.colorValue);
            return true;
        }
        return false;
    }

    protected boolean isThisPipeBlock(Block block) {
        return block != null && block.getClass().isAssignableFrom(getClass());
    }

    /**
     * Just returns proper pipe tile entity
     */
    public IPipeTile<PipeType, NodeDataType> getPipeTileEntity(IBlockAccess world, BlockPos selfPos) {
        return getPipeTileEntity(world.getTileEntity(selfPos));
    }

    @SuppressWarnings("unchecked")
    public IPipeTile<PipeType, NodeDataType> getPipeTileEntity(TileEntity tileEntity) {
        return tileEntity instanceof IPipeTile pipeTile && isThisPipeBlock(pipeTile.getPipeBlock()) ?
                (IPipeTile<PipeType, NodeDataType>) pipeTile : null;
    }

    public boolean canConnect(IPipeTile<PipeType, NodeDataType> selfTile, EnumFacing facing) {
        if (selfTile.getPipeWorld().getBlockState(selfTile.getPipePos().offset(facing)).getBlock() == Blocks.AIR)
            return false;
        CoverBehavior cover = selfTile.getCoverableImplementation().getCoverAtSide(facing);
        if (cover != null && !cover.canPipePassThrough()) {
            return false;
        }
        TileEntity other = selfTile.getPipeWorld().getTileEntity(selfTile.getPipePos().offset(facing));
        if (other instanceof IPipeTile pipeTile) {
            cover = pipeTile.getCoverableImplementation().getCoverAtSide(facing.getOpposite());
            //noinspection unchecked
            return (cover == null || cover.canPipePassThrough()) &&
                    canPipesConnect(selfTile, facing, pipeTile);
        }
        return canPipeConnectToBlock(selfTile, facing, other);
    }

    public abstract boolean canPipesConnect(IPipeTile<PipeType, NodeDataType> selfTile, EnumFacing side, IPipeTile<PipeType, NodeDataType> sideTile);

    public abstract boolean canPipeConnectToBlock(IPipeTile<PipeType, NodeDataType> selfTile, EnumFacing side, @Nullable TileEntity tile);

    private List<IndexedCuboid6> getCollisionBox(IBlockAccess world, BlockPos pos, @Nullable Entity entityIn) {
        IPipeTile<PipeType, NodeDataType> pipeTile = getPipeTileEntity(world, pos);
        if (pipeTile == null) {
            return Collections.emptyList();
        }
        if (pipeTile.getFrameMaterial() != null) {
            return Collections.singletonList(FULL_CUBE_COLLISION);
        }
        PipeType pipeType = pipeTile.getPipeType();
        if (pipeType == null) {
            return Collections.emptyList();
        }
        int actualConnections = getPipeTileEntity(world, pos).getVisualConnections();
        float thickness = pipeType.getThickness();
        ArrayList<IndexedCuboid6> result = new ArrayList<>();
        ICoverable coverable = pipeTile.getCoverableImplementation();

        // Check if the machine grid is being rendered
        if (hasPipeCollisionChangingItem(world, pos, entityIn)) {
            result.add(FULL_CUBE_COLLISION);
        }

        // Always add normal collision so player doesn't "fall through" the cable/pipe when
        // a tool is put in hand, and will still be standing where they were before.
        result.add(new IndexedCuboid6(new PrimaryBoxData(true), getSideBox(null, thickness)));
        for (EnumFacing side : EnumFacing.VALUES) {
            if ((actualConnections & 1 << side.getIndex()) != 0) {
                result.add(new IndexedCuboid6(new PipeConnectionData(side), getSideBox(side, thickness)));
            }
        }
        coverable.addCoverCollisionBoundingBox(result);
        return result;
    }

    public boolean hasPipeCollisionChangingItem(IBlockAccess world, BlockPos pos, Entity entity) {
        if (entity instanceof EntityPlayer player) {
            return hasPipeCollisionChangingItem(world, pos, player.getHeldItem(EnumHand.MAIN_HAND)) ||
                    hasPipeCollisionChangingItem(world, pos, player.getHeldItem(EnumHand.OFF_HAND)) ||
                    player.isSneaking() && isHoldingPipe(player);
        }
        return false;
    }

    public abstract boolean isHoldingPipe(EntityPlayer player);

    public boolean hasPipeCollisionChangingItem(IBlockAccess world, BlockPos pos, ItemStack stack) {
        return isPipeTool(stack) || ToolHelper.isTool(stack, ToolClasses.SCREWDRIVER) ||
                GTUtility.isCoverBehaviorItem(stack, () -> hasCover(getPipeTileEntity(world, pos)),
                        coverDef -> ICoverable.canPlaceCover(coverDef, getPipeTileEntity(world, pos).getCoverableImplementation()));
    }

    protected boolean hasCover(IPipeTile<PipeType, NodeDataType> pipeTile) {
        if (pipeTile == null)
            return false;
        return pipeTile.getCoverableImplementation().hasAnyCover();
    }

    @Override
    public boolean canRenderInLayer(@Nonnull IBlockState state, @Nonnull BlockRenderLayer layer) {
        return true;
    }

    @Nonnull
    @Override
    public IBlockState getFacade(@Nonnull IBlockAccess world, @Nonnull BlockPos pos, @Nullable EnumFacing side) {
        IPipeTile<?, ?> pipeTile = getPipeTileEntity(world, pos);
        if (pipeTile != null) {
            if (side != null && pipeTile.getCoverableImplementation().getCoverAtSide(side) instanceof IFacadeCover facadeCover) {
                return facadeCover.getVisualState();
            }
            Material frameMaterial = pipeTile.getFrameMaterial();
            if (frameMaterial != null) {
                return MetaBlocks.FRAMES.get(frameMaterial).getBlock(frameMaterial);
            }
        }
        return world.getBlockState(pos);
    }

    @Nonnull
    @Override
    public IBlockState getVisualState(@Nonnull IBlockAccess world, @Nonnull BlockPos pos, @Nonnull EnumFacing side) {
        return getFacade(world, pos, side);
    }

    @Override
    public boolean supportsVisualConnections() {
        return true;
    }

    @Nonnull
    @Override
    public BlockRenderLayer getRenderLayer() {
        return BlockRenderLayer.CUTOUT_MIPPED;
    }

    @Override
    @SuppressWarnings("deprecation")
    public boolean isOpaqueCube(@Nonnull IBlockState state) {
        return false;
    }

    @Override
    @SuppressWarnings("deprecation")
    public boolean isFullCube(@Nonnull IBlockState state) {
        return false;
    }

    @Override
    @SideOnly(Side.CLIENT)
    protected Pair<TextureAtlasSprite, Integer> getParticleTexture(World world, BlockPos blockPos) {
        return Pair.of(TextureUtils.getMissingSprite(), 0xFFFFFF); // TODO
    }

    @Override
    @Nonnull
    public IBlockState getExtendedState(@Nonnull IBlockState state, @Nonnull IBlockAccess world, @Nonnull BlockPos pos) {
        return Loader.isModLoaded(GTValues.MODID_CTM) ?
                new CTMSpecialState(state, world, pos) :
                new SimpleSpecialState(state, world, pos);
    }

    public static class PipeConnectionData {
        public final EnumFacing side;

        public PipeConnectionData(EnumFacing side) {
            this.side = side;
        }
    }
}

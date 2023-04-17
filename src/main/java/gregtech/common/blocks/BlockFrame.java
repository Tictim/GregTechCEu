package gregtech.common.blocks;

import gregtech.api.GregTechAPI;
import gregtech.api.block.DelayedStateBlock;
import gregtech.api.items.toolitem.ToolClasses;
import gregtech.api.items.toolitem.ToolHelper;
import gregtech.api.pipenet.block.BlockPipe;
import gregtech.api.pipenet.block.ItemBlockPipe;
import gregtech.api.pipenet.tile.IPipeTile;
import gregtech.api.pipenet.tile.TileEntityPipeBase;
import gregtech.api.recipes.ModHandler;
import gregtech.api.unification.material.Material;
import gregtech.api.unification.material.Materials;
import gregtech.api.unification.material.info.MaterialIconSet;
import gregtech.api.unification.material.info.MaterialIconType;
import gregtech.api.util.GTLog;
import gregtech.api.util.GTUtility;
import gregtech.client.model.modelfactories.MaterialBlockModelLoader;
import gregtech.common.blocks.properties.PropertyMaterial;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.EnumPushReaction;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving.SpawnPlacementType;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.*;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Map;

public final class BlockFrame extends DelayedStateBlock {

    private static final double CLIMBABLE_HITBOX_OFFSET = 1.0 / 16;
    private static final AxisAlignedBB[] COLLISION_BOXES = new AxisAlignedBB[16];

    @Nonnull
    public static AxisAlignedBB getCollisionBox() {
        return getCollisionBox(0b1111);
    }

    /**
     * Get collision box for the frame.
     *
     * @param sideClimbableMask A flag set denoting which side of the frame can be climbed.
     *                          For each horizontal facings, a value of {@code 1} at the
     *                          position of corresponding horizontal index indicates the
     *                          side is climbable.
     * @return Collision box
     */
    @Nonnull
    public static AxisAlignedBB getCollisionBox(int sideClimbableMask) {
        sideClimbableMask &= 0b1111;
        AxisAlignedBB box = COLLISION_BOXES[sideClimbableMask];
        if (box == null) {
            return COLLISION_BOXES[sideClimbableMask] =
                    new AxisAlignedBB(
                            (sideClimbableMask & 2) != 0 ? CLIMBABLE_HITBOX_OFFSET : 0, // west
                            0.0,
                            (sideClimbableMask & 4) != 0 ? CLIMBABLE_HITBOX_OFFSET : 0, // north
                            (sideClimbableMask & 8) != 0 ? 1 - CLIMBABLE_HITBOX_OFFSET : 1, // east
                            1.0,
                            (sideClimbableMask & 1) != 0 ? 1 - CLIMBABLE_HITBOX_OFFSET : 1); // south
        }
        return box;
    }

    public final PropertyMaterial variantProperty;

    // todo wood?
    public BlockFrame(Material[] materials) {
        super(net.minecraft.block.material.Material.IRON);
        setTranslationKey("frame");
        setHardness(3.0f);
        setResistance(6.0f);
        setCreativeTab(GregTechAPI.TAB_GREGTECH_MATERIALS);
        this.variantProperty = PropertyMaterial.create("variant", materials);
        initBlockState();
    }

    @Override
    public int damageDropped(@Nonnull IBlockState state) {
        return getMetaFromState(state);
    }

    @Nonnull
    @Override
    @SuppressWarnings("deprecation")
    public IBlockState getStateFromMeta(int meta) {
        if (meta >= variantProperty.getAllowedValues().size()) {
            meta = 0;
        }
        return getDefaultState().withProperty(variantProperty, variantProperty.getAllowedValues().get(meta));
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        return variantProperty.getAllowedValues().indexOf(state.getValue(variantProperty));
    }

    @Override
    public String getHarvestTool(IBlockState state) {
        Material material = state.getValue(variantProperty);
        if (ModHandler.isMaterialWood(material)) {
            return ToolClasses.AXE;
        }
        return ToolClasses.WRENCH;
    }

    @Nonnull
    @Override
    public SoundType getSoundType(IBlockState state, @Nonnull World world, @Nonnull BlockPos pos, @Nullable Entity entity) {
        Material material = state.getValue(variantProperty);
        if (ModHandler.isMaterialWood(material)) {
            return SoundType.WOOD;
        }
        return SoundType.METAL;
    }

    public SoundType getSoundType(ItemStack stack) {
        Material material = getGtMaterial(stack.getMetadata());
        if (ModHandler.isMaterialWood(material)) {
            return SoundType.WOOD;
        }
        return SoundType.METAL;
    }

    @Override
    public int getHarvestLevel(@Nonnull IBlockState state) {
        return 1;
    }

    @Override
    protected BlockStateContainer createStateContainer() {
        return new BlockStateContainer(this, variantProperty);
    }

    @Override
    @Nonnull
    @SuppressWarnings("deprecation")
    public net.minecraft.block.material.Material getMaterial(IBlockState state) {
        Material material = state.getValue(variantProperty);
        if (ModHandler.isMaterialWood(material)) {
            return net.minecraft.block.material.Material.WOOD;
        }
        return super.getMaterial(state);
    }

    @Override
    public void getSubBlocks(@Nonnull CreativeTabs tab, @Nonnull NonNullList<ItemStack> list) {
        blockState.getValidStates().stream()
                .filter(blockState -> blockState.getValue(variantProperty) != Materials.NULL)
                .forEach(blockState -> list.add(GTUtility.toItem(blockState)));
    }

    public ItemStack getItem(Material material) {
        return GTUtility.toItem(getDefaultState().withProperty(variantProperty, material));
    }

    public IBlockState getBlock(Material material) {
        return getDefaultState().withProperty(variantProperty, material);
    }

    public Material getGtMaterial(int meta) {
        return variantProperty.getAllowedValues().get(meta);
    }

    @Override
    public boolean canCreatureSpawn(@Nonnull IBlockState state, @Nonnull IBlockAccess world, @Nonnull BlockPos pos, @Nonnull SpawnPlacementType type) {
        return false;
    }

    public boolean replaceWithFramedPipe(World worldIn, BlockPos pos, IBlockState state, EntityPlayer playerIn, ItemStack stackInHand, EnumFacing facing) {
        BlockPipe<?, ?, ?> blockPipe = (BlockPipe<?, ?, ?>) ((ItemBlockPipe<?, ?>) stackInHand.getItem()).getBlock();
        if (blockPipe.getItemPipeType(stackInHand).getThickness() < 1) {
            ItemBlock itemBlock = (ItemBlock) stackInHand.getItem();
            IBlockState pipeState = blockPipe.getDefaultState();
            // these 0 values are not actually used by forge
            itemBlock.placeBlockAt(stackInHand, playerIn, worldIn, pos, facing, 0, 0, 0, pipeState);
            IPipeTile<?, ?> pipeTile = blockPipe.getPipeTileEntity(worldIn, pos);
            if (pipeTile instanceof TileEntityPipeBase) {
                ((TileEntityPipeBase<?, ?>) pipeTile).setFrameMaterial(getGtMaterial(getMetaFromState(state)));
            } else {
                GTLog.logger.error("Pipe was not placed!");
                return false;
            }
            SoundType type = blockPipe.getSoundType(state, worldIn, pos, playerIn);
            worldIn.playSound(playerIn, pos, type.getPlaceSound(), SoundCategory.BLOCKS, (type.getVolume() + 1.0F) / 2.0F, type.getPitch() * 0.8F);
            if (!playerIn.capabilities.isCreativeMode) {
                stackInHand.shrink(1);
            }
            return true;
        }
        return false;
    }

    public boolean removeFrame(World world, BlockPos pos, EntityPlayer player, ItemStack stack) {
        TileEntity te = world.getTileEntity(pos);
        if (te instanceof TileEntityPipeBase<?, ?> && ((IPipeTile<?, ?>) te).getFrameMaterial() != null) {
            TileEntityPipeBase<?, ?> pipeTile = (TileEntityPipeBase<?, ?>) te;
            Material frameMaterial = pipeTile.getFrameMaterial();
            pipeTile.setFrameMaterial(null);
            Block.spawnAsEntity(world, pos, this.getItem(frameMaterial));
            ToolHelper.damageItem(stack, player);
            ToolHelper.playToolSound(stack, player);
            return true;
        }
        return false;
    }

    @Override
    public boolean onBlockActivated(@Nonnull World worldIn, @Nonnull BlockPos pos, @Nonnull IBlockState state, EntityPlayer playerIn, @Nonnull EnumHand hand, @Nonnull EnumFacing facing, float hitX, float hitY, float hitZ) {
        ItemStack stackInHand = playerIn.getHeldItem(hand);
        if (stackInHand.isEmpty()) {
            return false;
        }
        // replace frame with pipe and set the frame material to this frame
        if (stackInHand.getItem() instanceof ItemBlockPipe) {
            return replaceWithFramedPipe(worldIn, pos, state, playerIn, stackInHand, facing);
        }

        if (stackInHand.getItem().getToolClasses(stackInHand).contains(ToolClasses.CROWBAR)) {
            return removeFrame(worldIn, pos, playerIn, stackInHand);
        }

        if (!(stackInHand.getItem() instanceof FrameItemBlock)) {
            return false;
        }
        BlockPos.PooledMutableBlockPos blockPos = BlockPos.PooledMutableBlockPos.retain();
        blockPos.setPos(pos);
        for (int i = 0; i < 32; i++) {
            if (worldIn.getBlockState(blockPos).getBlock() instanceof BlockFrame) {
                blockPos.move(EnumFacing.UP);
                continue;
            }
            TileEntity te = worldIn.getTileEntity(blockPos);
            if (te instanceof IPipeTile && ((IPipeTile<?, ?>) te).getFrameMaterial() != null) {
                blockPos.move(EnumFacing.UP);
                continue;
            }
            if (canPlaceBlockAt(worldIn, blockPos)) {
                worldIn.setBlockState(blockPos, ((FrameItemBlock) stackInHand.getItem()).getBlockState(stackInHand));
                SoundType type = getSoundType(stackInHand);
                worldIn.playSound(null, pos, type.getPlaceSound(), SoundCategory.BLOCKS, (type.getVolume() + 1.0F) / 2.0F, type.getPitch() * 0.8F);
                if (!playerIn.capabilities.isCreativeMode) {
                    stackInHand.shrink(1);
                }
                blockPos.release();
                return true;
            } else if (te instanceof TileEntityPipeBase && ((TileEntityPipeBase<?, ?>) te).getFrameMaterial() == null) {
                Material material = ((BlockFrame) ((FrameItemBlock) stackInHand.getItem()).getBlock()).getGtMaterial(stackInHand.getMetadata());
                ((TileEntityPipeBase<?, ?>) te).setFrameMaterial(material);
                SoundType type = getSoundType(stackInHand);
                worldIn.playSound(null, pos, type.getPlaceSound(), SoundCategory.BLOCKS, (type.getVolume() + 1.0F) / 2.0F, type.getPitch() * 0.8F);
                if (!playerIn.capabilities.isCreativeMode) {
                    stackInHand.shrink(1);
                }
                blockPos.release();
                return true;
            } else {
                blockPos.release();
                return false;
            }
        }
        blockPos.release();
        return false;
    }

    @Override
    public void onEntityCollision(@Nonnull World worldIn, @Nonnull BlockPos pos, @Nonnull IBlockState state, Entity entityIn) {
        entityIn.motionX = MathHelper.clamp(entityIn.motionX, -0.15, 0.15);
        entityIn.motionZ = MathHelper.clamp(entityIn.motionZ, -0.15, 0.15);
        entityIn.fallDistance = 0.0F;
        if (entityIn.motionY < -0.15D) {
            entityIn.motionY = -0.15D;
        }
        if (entityIn.isSneaking() && entityIn.motionY < 0.0D) {
            entityIn.motionY = 0.0D;
        }
        if (entityIn.collidedHorizontally) {
            entityIn.motionY = 0.3;
        }
    }

    @Override
    @SuppressWarnings("deprecation")
    public boolean causesSuffocation(IBlockState state) {
        return false;
    }

    @Nonnull
    @Override
    @SuppressWarnings("deprecation")
    public EnumPushReaction getPushReaction(@Nonnull IBlockState state) {
        return EnumPushReaction.DESTROY;
    }

    @Override
    @SuppressWarnings("deprecation")
    public AxisAlignedBB getCollisionBoundingBox(@Nonnull IBlockState blockState, @Nonnull IBlockAccess worldIn, @Nonnull BlockPos pos) {
        return getCollisionBox();
    }

    @Nonnull
    @Override
    public BlockRenderLayer getRenderLayer() {
        return BlockRenderLayer.CUTOUT_MIPPED;
    }

    @Override
    @SuppressWarnings("deprecation")
    public boolean isFullCube(IBlockState state) {
        return false;
    }

    @Override
    @SuppressWarnings("deprecation")
    public boolean isOpaqueCube(@Nonnull IBlockState state) {
        return false;
    }

    @Nonnull
    @Override
    @SuppressWarnings("deprecation")
    public BlockFaceShape getBlockFaceShape(@Nonnull IBlockAccess worldIn, @Nonnull IBlockState state, @Nonnull BlockPos pos, @Nonnull EnumFacing face) {
        return BlockFaceShape.UNDEFINED;
    }

    @Override
    @SuppressWarnings("deprecation")
    public boolean shouldSideBeRendered(IBlockState state, IBlockAccess world, BlockPos pos, EnumFacing side) {
        return shouldFrameSideBeRendered(state.getValue(this.variantProperty), world, pos, side);
    }

    public static boolean shouldFrameSideBeRendered(Material frameMaterial, IBlockAccess world, BlockPos pos, EnumFacing side) {
        BlockPos offset = pos.offset(side);
        IBlockState sideState = world.getBlockState(offset);
        Material frameMaterialAt = getFrameMaterialAt(world, sideState, offset);
        if (frameMaterialAt != null) {
            MaterialIconSet icon1 = frameMaterial.getMaterialIconSet();
            MaterialIconSet icon2 = frameMaterialAt.getMaterialIconSet();
            if (icon1 == icon2 || MaterialIconType.frameGt.getBlockstatesPath(icon1) == MaterialIconType.frameGt.getBlockstatesPath(icon2)) {
                return false;
            }
        }
        return !sideState.doesSideBlockRendering(world, offset, side.getOpposite());
    }

    @Nullable
    public static Material getFrameMaterialAt(IBlockAccess world, BlockPos pos) {
        return getFrameMaterialAt(world, world.getBlockState(pos), pos);
    }

    @Nullable
    public static Material getFrameMaterialAt(IBlockAccess world, IBlockState state, BlockPos pos) {
        if (state.getBlock() instanceof BlockFrame) {
            return state.getValue(((BlockFrame) state.getBlock()).variantProperty);
        } else if (state.getBlock() instanceof BlockPipe) {
            TileEntity te = world.getTileEntity(pos);
            if (te instanceof IPipeTile) {
                return ((IPipeTile<?, ?>) te).getFrameMaterial();
            }
        }
        return null;
    }

    @SideOnly(Side.CLIENT)
    public void onModelRegister() {
        Map<IBlockState, ModelResourceLocation> map = new Object2ObjectOpenHashMap<>();
        for (IBlockState state : this.getBlockState().getValidStates()) {
            MaterialBlockModelLoader.Entry entry = new MaterialBlockModelLoader.EntryBuilder(
                    MaterialIconType.frameGt,
                    state.getValue(this.variantProperty).getMaterialIconSet())
                    .register();
            map.put(state, entry.getBlockModelId());

            ModelLoader.setCustomModelResourceLocation(Item.getItemFromBlock(this),
                    this.getMetaFromState(state),
                    entry.getItemModelId());
        }
        ModelLoader.setCustomStateMapper(this, b -> map);
    }
}

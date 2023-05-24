package gregtech.common.blocks;

import gregtech.api.GTValues;
import gregtech.api.GregTechAPI;
import gregtech.api.items.toolitem.ToolClasses;
import gregtech.api.items.toolitem.ToolHelper;
import gregtech.api.pipenet.block.BlockPipe;
import gregtech.api.pipenet.block.ItemBlockPipe;
import gregtech.api.pipenet.tile.IPipeTile;
import gregtech.api.pipenet.tile.TileEntityPipeBase;
import gregtech.api.recipes.ModHandler;
import gregtech.api.unification.material.Material;
import gregtech.api.unification.material.info.MaterialIconType;
import gregtech.api.util.GTLog;
import gregtech.client.model.MaterialStateMapper;
import gregtech.client.model.modelfactories.MaterialBlockModelLoader;
import gregtech.common.ConfigHolder;
import gregtech.common.blocks.properties.PropertyMaterial;
import gregtech.common.blocks.special.CTMSpecialState;
import gregtech.common.blocks.special.SimpleSpecialState;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.EnumPushReaction;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving.SpawnPlacementType;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;

public abstract class BlockFrame extends BlockMaterialBase {

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

    public static BlockFrame create(Material[] materials) {
        PropertyMaterial property = PropertyMaterial.create("variant", materials);
        return new BlockFrame() {
            @Nonnull
            @Override
            public PropertyMaterial getVariantProperty() {
                return property;
            }
        };
    }

    private BlockFrame() {
        super(net.minecraft.block.material.Material.IRON);
        setTranslationKey("frame");
        setHardness(3.0f);
        setResistance(6.0f);
        setCreativeTab(GregTechAPI.TAB_GREGTECH_MATERIALS);
    }

    @Override
    public String getHarvestTool(IBlockState state) {
        Material material = getGtMaterial(state);
        if (ModHandler.isMaterialWood(material)) {
            return ToolClasses.AXE;
        }
        return ToolClasses.WRENCH;
    }

    @Nonnull
    @Override
    public SoundType getSoundType(IBlockState state, @Nonnull World world, @Nonnull BlockPos pos, @Nullable Entity entity) {
        Material material = getGtMaterial(state);
        if (ModHandler.isMaterialWood(material)) {
            return SoundType.WOOD;
        }
        return SoundType.METAL;
    }

    public SoundType getSoundType(ItemStack stack) {
        Material material = getGtMaterial(stack);
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
    @Nonnull
    @SuppressWarnings("deprecation")
    public net.minecraft.block.material.Material getMaterial(IBlockState state) {
        Material material = getGtMaterial(state);
        if (ModHandler.isMaterialWood(material)) {
            return net.minecraft.block.material.Material.WOOD;
        }
        return super.getMaterial(state);
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
            if (blockPipe.getPipeTileEntity(worldIn, pos) instanceof TileEntityPipeBase pipeBase) {
                pipeBase.setFrameMaterial(getGtMaterial(state));
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
        if (te instanceof TileEntityPipeBase<?, ?> pipeTile && pipeTile.getFrameMaterial() != null) {
            Material frameMaterial = pipeTile.getFrameMaterial();
            if (frameMaterial != null) {
                pipeTile.setFrameMaterial(null);
                Block.spawnAsEntity(world, pos, this.getItem(frameMaterial));
                ToolHelper.damageItem(stack, player);
                ToolHelper.playToolSound(stack, player);
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean onBlockActivated(@Nonnull World world, @Nonnull BlockPos pos, @Nonnull IBlockState state,
                                    @Nonnull EntityPlayer player, @Nonnull EnumHand hand, @Nonnull EnumFacing facing,
                                    float hitX, float hitY, float hitZ) {
        ItemStack stack = player.getHeldItem(hand);
        if (stack.isEmpty()) {
            return false;
        }
        // replace frame with pipe and set the frame material to this frame
        if (stack.getItem() instanceof ItemBlockPipe) {
            return replaceWithFramedPipe(world, pos, state, player, stack, facing);
        }

        if (stack.getItem().getToolClasses(stack).contains(ToolClasses.CROWBAR)) {
            return removeFrame(world, pos, player, stack);
        }

        BlockFrame frameBlock = getFrameBlockFromItem(stack);
        if (frameBlock == null) return false;

        BlockPos.PooledMutableBlockPos blockPos = BlockPos.PooledMutableBlockPos.retain();
        blockPos.setPos(pos);
        for (int i = 0; i < 32; i++) {
            if (world.getBlockState(blockPos).getBlock() instanceof BlockFrame) {
                blockPos.move(EnumFacing.UP);
                continue;
            }
            TileEntity te = world.getTileEntity(blockPos);
            if (te instanceof IPipeTile pipeTile && pipeTile.getFrameMaterial() != null) {
                blockPos.move(EnumFacing.UP);
                continue;
            }
            if (canPlaceBlockAt(world, blockPos)) {
                world.setBlockState(blockPos, this.getStateFromMeta(stack.getItem().getMetadata(stack.getItemDamage())));
                SoundType type = getSoundType(stack);
                world.playSound(null, pos, type.getPlaceSound(), SoundCategory.BLOCKS, (type.getVolume() + 1.0F) / 2.0F, type.getPitch() * 0.8F);
                if (!player.capabilities.isCreativeMode) {
                    stack.shrink(1);
                }
                blockPos.release();
                return true;
            } else if (te instanceof TileEntityPipeBase pipeBase && pipeBase.getFrameMaterial() == null) {
                pipeBase.setFrameMaterial(frameBlock.getGtMaterial(stack));
                SoundType type = getSoundType(stack);
                world.playSound(null, pos, type.getPlaceSound(), SoundCategory.BLOCKS, (type.getVolume() + 1.0F) / 2.0F, type.getPitch() * 0.8F);
                if (!player.capabilities.isCreativeMode) {
                    stack.shrink(1);
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
        return shouldFrameSideBeRendered(getGtMaterial(state), world, pos, side);
    }

    public static boolean shouldFrameSideBeRendered(@Nonnull Material frameMaterial, @Nonnull IBlockAccess world, @Nonnull BlockPos pos, @Nonnull EnumFacing side) {
        BlockPos offset = pos.offset(side);
        IBlockState sideState = world.getBlockState(offset);
        Material frameMaterialAt = getFrameMaterialAt(world, sideState, offset);
        return frameMaterial != frameMaterialAt && !sideState.doesSideBlockRendering(world, offset, side.getOpposite());
    }

    @Nullable
    public static Material getFrameMaterialAt(@Nonnull IBlockAccess world, @Nonnull BlockPos pos) {
        return getFrameMaterialAt(world, world.getBlockState(pos), pos);
    }

    @Nullable
    public static Material getFrameMaterialAt(@Nonnull IBlockAccess world, @Nonnull IBlockState state, @Nonnull BlockPos pos) {
        if (state.getBlock() instanceof BlockFrame frame) {
            return frame.getGtMaterial(state);
        }
        if (state.getBlock() instanceof BlockPipe && world.getTileEntity(pos) instanceof IPipeTile pipeTile) {
            return pipeTile.getFrameMaterial();
        }
        return null;
    }

    @Override
    @Nonnull
    public IBlockState getExtendedState(@Nonnull IBlockState state, @Nonnull IBlockAccess world, @Nonnull BlockPos pos) {
        return Loader.isModLoaded(GTValues.MODID_CTM) ?
                new CTMSpecialState(state, world, pos) :
                new SimpleSpecialState(state, world, pos);
    }

    @Override
    public void addInformation(ItemStack stack, @Nullable World world, List<String> tooltip, ITooltipFlag flag) {
        if (ConfigHolder.misc.debug) {
            tooltip.add("MetaItem Id: frame" + getGtMaterial(stack).toCamelCaseString());
        }
    }

    @SideOnly(Side.CLIENT)
    public void onModelRegister() {
        Map<Material, MaterialBlockModelLoader.Entry> map = new Object2ObjectOpenHashMap<>();
        for (IBlockState state : this.getBlockState().getValidStates()) {
            Material material = getGtMaterial(state);
            MaterialBlockModelLoader.Entry entry = new MaterialBlockModelLoader.EntryBuilder(
                    MaterialIconType.frameGt,
                    material.getMaterialIconSet())
                    .setStateProperties("")
                    .register();
            map.put(material, entry);

            ModelLoader.setCustomModelResourceLocation(Item.getItemFromBlock(this),
                    this.getMetaFromState(state),
                    entry.getItemModelId());
        }
        ModelLoader.setCustomStateMapper(this, new MaterialStateMapper(map, this::getGtMaterial));
    }

    @Nullable
    public static BlockFrame getFrameBlockFromItem(ItemStack stack) {
        return stack.getItem() instanceof ItemBlock itemBlock &&
                itemBlock.getBlock() instanceof BlockFrame blockFrame ?
                blockFrame : null;
    }
}

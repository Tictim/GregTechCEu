package gregtech.common.blocks;

import gregtech.api.GregTechAPI;
import gregtech.api.block.DelayedStateBlock;
import gregtech.api.items.toolitem.ToolClasses;
import gregtech.api.unification.material.Material;
import gregtech.api.unification.material.Materials;
import gregtech.api.unification.material.info.MaterialIconType;
import gregtech.api.unification.material.properties.PropertyKey;
import gregtech.api.util.GTUtility;
import gregtech.client.model.modelfactories.MaterialBlockModelLoader;
import gregtech.common.blocks.properties.PropertyMaterial;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.Entity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Map;

public final class BlockCompressed extends DelayedStateBlock {

    public final PropertyMaterial variantProperty;

    public BlockCompressed(Material[] materials) {
        super(net.minecraft.block.material.Material.IRON);
        setTranslationKey("compressed");
        setHardness(5.0f);
        setResistance(10.0f);
        setCreativeTab(GregTechAPI.TAB_GREGTECH_MATERIALS);
        this.variantProperty = PropertyMaterial.create("variant", materials);
        initBlockState();
    }

    @Override
    public int damageDropped(@Nonnull IBlockState state) {
        return getMetaFromState(state);
    }

    @Override
    public String getHarvestTool(IBlockState state) {
        Material material = state.getValue(variantProperty);
        if (material.isSolid()) {
            return ToolClasses.PICKAXE;
        } else if (material.hasProperty(PropertyKey.DUST)) {
            return ToolClasses.SHOVEL;
        }
        return ToolClasses.PICKAXE;
    }

    @Override
    public int getHarvestLevel(IBlockState state) {
        Material material = state.getValue(variantProperty);
        if (material.hasProperty(PropertyKey.DUST)) {
            return material.getBlockHarvestLevel();
        }
        return 0;
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
    protected BlockStateContainer createStateContainer() {
        return new BlockStateContainer(this, variantProperty);
    }

    public static ItemStack getItem(IBlockState blockState) {
        return GTUtility.toItem(blockState);
    }

    public ItemStack getItem(Material material) {
        return getItem(getDefaultState().withProperty(variantProperty, material));
    }

    public Material getGtMaterial(int meta) {
        return variantProperty.getAllowedValues().get(meta);
    }

    public IBlockState getBlock(Material material) {
        return getDefaultState().withProperty(variantProperty, material);
    }

    @Override
    public void getSubBlocks(@Nonnull CreativeTabs tab, @Nonnull NonNullList<ItemStack> list) {
        blockState.getValidStates().stream()
                .filter(blockState -> blockState.getValue(variantProperty) != Materials.NULL)
                .forEach(blockState -> list.add(getItem(blockState)));
    }

    @Override
    @Nonnull
    @SuppressWarnings("deprecation")
    public net.minecraft.block.material.Material getMaterial(IBlockState state) {
        Material material = state.getValue(variantProperty);
        if (material.hasProperty(PropertyKey.GEM)) {
            return net.minecraft.block.material.Material.ROCK;
        } else if (material.hasProperty(PropertyKey.INGOT)) {
            return net.minecraft.block.material.Material.IRON;
        } else if (material.hasProperty(PropertyKey.DUST)) {
            return net.minecraft.block.material.Material.SAND;
        }
        return net.minecraft.block.material.Material.ROCK;
    }

    @Nonnull
    @Override
    @SuppressWarnings("deprecation")
    public MapColor getMapColor(@Nonnull IBlockState state, @Nonnull IBlockAccess worldIn, @Nonnull BlockPos pos) {
        return getMaterial(state).getMaterialMapColor();
    }

    @Nonnull
    @Override
    public SoundType getSoundType(IBlockState state, @Nonnull World world, @Nonnull BlockPos pos, @Nullable Entity entity) {
        Material material = state.getValue(variantProperty);
        if (material.hasProperty(PropertyKey.GEM)) {
            return SoundType.STONE;
        } else if (material.hasProperty(PropertyKey.INGOT)) {
            return SoundType.METAL;
        } else if (material.hasProperty(PropertyKey.DUST)) {
            return SoundType.SAND;
        }
        return SoundType.STONE;
    }

    @SideOnly(Side.CLIENT)
    public void onModelRegister() {
        Map<IBlockState, ModelResourceLocation> map = new Object2ObjectOpenHashMap<>();
        for (IBlockState state : this.getBlockState().getValidStates()) {
            MaterialBlockModelLoader.Entry entry = new MaterialBlockModelLoader.EntryBuilder(
                    MaterialIconType.block,
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

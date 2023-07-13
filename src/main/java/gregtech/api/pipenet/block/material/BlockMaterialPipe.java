package gregtech.api.pipenet.block.material;

import gregtech.api.GTValues;
import gregtech.api.pipenet.PipeNet;
import gregtech.api.pipenet.WorldPipeNet;
import gregtech.api.pipenet.block.BlockPipe;
import gregtech.api.pipenet.block.IPipeType;
import gregtech.api.pipenet.tile.IPipeTile;
import gregtech.api.pipenet.tile.TileEntityPipeBase;
import gregtech.api.unification.material.Material;
import gregtech.api.unification.material.info.MaterialIconType;
import gregtech.api.unification.material.registry.MaterialRegistry;
import gregtech.api.unification.ore.OrePrefix;
import gregtech.client.model.SimpleStateMapper;
import gregtech.client.model.modelfactories.BakedModelHandler;
import gregtech.client.model.pipe.PipeModelSelector;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nonnull;
import java.util.Set;

public abstract class BlockMaterialPipe<PipeType extends Enum<PipeType> & IPipeType<NodeDataType> & IMaterialPipeType<NodeDataType>, NodeDataType, WorldPipeNetType extends WorldPipeNet<NodeDataType, ? extends PipeNet<NodeDataType>>> extends BlockPipe<PipeType, NodeDataType, WorldPipeNetType> {

    protected final PipeType pipeType;
    private final MaterialRegistry registry;

    public BlockMaterialPipe(@Nonnull PipeType pipeType, @Nonnull MaterialRegistry registry) {
        this.pipeType = pipeType;
        this.registry = registry;
    }

    @Override
    public NodeDataType createProperties(IPipeTile<PipeType, NodeDataType> pipeTile) {
        PipeType pipeType = pipeTile.getPipeType();
        Material material = ((IMaterialPipeTile<PipeType, NodeDataType>) pipeTile).getPipeMaterial();
        if (pipeType == null || material == null) {
            return getFallbackType();
        }
        return createProperties(pipeType, material);
    }

    @Override
    public NodeDataType createItemProperties(ItemStack itemStack) {
        Material material = getItemMaterial(itemStack);
        if (pipeType == null || material == null) {
            return getFallbackType();
        }
        return createProperties(pipeType, material);
    }

    public ItemStack getItem(Material material) {
        if (material == null) return ItemStack.EMPTY;
        int materialId = registry.getIDForObject(material);
        return new ItemStack(this, 1, materialId);
    }

    public Material getItemMaterial(ItemStack itemStack) {
        return registry.getObjectById(itemStack.getMetadata());
    }

    @Override
    public void setTileEntityData(TileEntityPipeBase<PipeType, NodeDataType> pipeTile, ItemStack itemStack) {
        ((TileEntityMaterialPipeBase<PipeType, NodeDataType>) pipeTile).setPipeData(this, pipeType, getItemMaterial(itemStack));
    }

    @Override
    public ItemStack getDropItem(IPipeTile<PipeType, NodeDataType> pipeTile) {
        Material material = ((IMaterialPipeTile<PipeType, NodeDataType>) pipeTile).getPipeMaterial();
        return getItem(material);
    }

    protected abstract NodeDataType createProperties(PipeType pipeType, Material material);

    public OrePrefix getPrefix() {
        return pipeType.getOrePrefix();
    }

    public PipeType getItemPipeType(ItemStack is) {
        return pipeType;
    }

    @Nonnull
    public MaterialRegistry getMaterialRegistry() {
        return registry;
    }

    @SideOnly(Side.CLIENT)
    @Nonnull
    public abstract Set<Material> getEnabledMaterials();

    @Nonnull
    protected abstract MaterialIconType getIconType();

    public void onModelRegister() {
        ModelResourceLocation modelId = new ModelResourceLocation(
                GTValues.MODID + ":material_pipe_" + getIconType().name + "_" + this.pipeType.getName());
        BakedModelHandler.addCustomBakedModel(modelId,
                new PipeModelSelector(getIconType(), getEnabledMaterials(), this.pipeType, this::getItemMaterial));

        ModelLoader.setCustomStateMapper(this, new SimpleStateMapper(modelId));

        Item item = Item.getItemFromBlock(this);
        ModelLoader.setCustomMeshDefinition(item, stack -> modelId);
        ModelLoader.registerItemVariants(item);
    }
}

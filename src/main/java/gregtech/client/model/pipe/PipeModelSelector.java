package gregtech.client.model.pipe;

import codechicken.lib.texture.TextureUtils;
import gregtech.api.pipenet.block.IPipeType;
import gregtech.api.pipenet.block.material.IMaterialPipeTile;
import gregtech.api.unification.material.Material;
import gregtech.api.unification.material.info.MaterialIconSet;
import gregtech.api.unification.material.info.MaterialIconType;
import gregtech.client.model.ISpecialBakedModel;
import gregtech.client.model.component.ModelStates;
import gregtech.client.model.modelfactories.MaterialBlockModelLoader;
import gregtech.common.blocks.special.ISpecialState;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ItemOverrideList;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import net.minecraftforge.client.model.IModel;
import net.minecraftforge.client.model.ModelLoaderRegistry;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

public class PipeModelSelector implements ISpecialBakedModel {

    private final Map<MaterialIconSet, ModelResourceLocation> models = new Object2ObjectOpenHashMap<>();
    private final Function<ItemStack, Material> itemMaterialFunction;

    @Nullable
    private IBakedModel missingno;

    private final ItemOverrideList itemModelSelector = new ItemOverrideList() {
        @Nonnull
        @Override
        public IBakedModel handleItemState(@Nonnull IBakedModel originalModel,
                                           @Nonnull ItemStack stack,
                                           @Nullable World world,
                                           @Nullable EntityLivingBase entity) {
            Material material = itemMaterialFunction.apply(stack);
            if (material != null) {
                ModelResourceLocation r = models.get(material.getMaterialIconSet());
                if (r != null) {
                    return Minecraft.getMinecraft().blockRenderDispatcher.getBlockModelShapes()
                            .getModelManager()
                            .getModel(r);
                }
            }

            return missingno;
        }
    };

    public PipeModelSelector(@Nonnull MaterialIconType iconType,
                             @Nonnull Collection<Material> materials,
                             @Nonnull IPipeType<?> pipeType,
                             @Nonnull Function<ItemStack, Material> itemMaterialFunction) {
        this.itemMaterialFunction = Objects.requireNonNull(itemMaterialFunction, "itemMaterialFunction == null");

        for (Material m : materials) {
            MaterialIconSet iconSet = m.getMaterialIconSet();
            if (this.models.containsKey(iconSet)) {
                continue;
            }
            ModelResourceLocation model = MaterialBlockModelLoader.loadBlockModel(iconType, m.getMaterialIconSet(),
                    "type=" + pipeType.getName());
            this.models.put(iconSet, model);
        }
    }

    @Nonnull
    @Override
    public ModelStates collectModels(@Nullable ISpecialState state) {
        ModelStates states = new ModelStates(state, null, isBloomActive());
        if (state != null) {
            if (state.getWorld().getTileEntity(state.getPos()) instanceof IMaterialPipeTile<?, ?> pipeTile) {
                Material m = pipeTile.getPipeMaterial();
                if (m != null) {
                    ModelResourceLocation r = this.models.get(m.getMaterialIconSet());
                    if (r != null) {
                        states.includeModel(Minecraft.getMinecraft().blockRenderDispatcher.getBlockModelShapes()
                                .getModelManager()
                                .getModel(r));
                        return states;
                    }
                }
            }
        }
        states.includeModel(getMissingModel());
        return states;
    }

    @Nonnull
    protected IBakedModel getMissingModel() {
        if (this.missingno == null) {
            IModel missingModel = ModelLoaderRegistry.getMissingModel();
            this.missingno = missingModel
                    .bake(missingModel.getDefaultState(),
                            DefaultVertexFormats.ITEM,
                            rl -> Minecraft.getMinecraft().getTextureMapBlocks().getAtlasSprite(rl.toString()));
        }
        return this.missingno;
    }

    @Override
    public boolean isAmbientOcclusion() {
        return true;
    }

    @Override
    public boolean isGui3d() {
        return true;
    }

    @Nonnull
    @Override
    public TextureAtlasSprite getParticleTexture() {
        return TextureUtils.getMissingSprite();
    }

    @Nonnull
    @Override
    public ItemOverrideList getOverrides() {
        return this.itemModelSelector;
    }
}

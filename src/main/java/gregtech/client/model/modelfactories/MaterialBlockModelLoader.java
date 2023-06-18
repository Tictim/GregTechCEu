package gregtech.client.model.modelfactories;

import gregtech.api.GTValues;
import gregtech.api.unification.material.info.MaterialIconSet;
import gregtech.api.unification.material.info.MaterialIconType;
import gregtech.api.util.GTLog;
import gregtech.api.util.GTUtility;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.registry.IRegistry;
import net.minecraftforge.client.event.ModelBakeEvent;
import net.minecraftforge.client.event.TextureStitchEvent;
import net.minecraftforge.client.model.IModel;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.client.model.ModelLoaderRegistry;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.ReflectionHelper;
import net.minecraftforge.fml.relauncher.Side;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Map;
import java.util.Objects;
import java.util.function.UnaryOperator;

@Mod.EventBusSubscriber(modid = GTValues.MODID, value = Side.CLIENT)
public class MaterialBlockModelLoader {

    private static final ObjectOpenHashSet<Entry> ENTRIES = new ObjectOpenHashSet<>();

    @SubscribeEvent
    public static void onTextureStitch(TextureStitchEvent.Pre event) {
        for (Entry e : ENTRIES) {
            e.blockModelCache = loadModel(event, e.getBlockModelLocation());
            e.itemModelCache = loadModel(event, e.getItemModelLocation());
        }
    }

    @Nullable
    private static IModel loadModel(TextureStitchEvent.Pre event, ResourceLocation modelLocation) {
        IModel model;
        try {
            model = ModelLoaderRegistry.getModel(modelLocation);
        } catch (Exception e) {
            GTLog.logger.error("Failed to load material model {}:", modelLocation, e);
            return null;
        }
        for (ResourceLocation texture : model.getTextures()) {
            event.getMap().registerSprite(texture);
        }
        return model;
    }

    @SuppressWarnings("deprecation")
    @SubscribeEvent
    public static void onModelBake(ModelBakeEvent event) {
        Map<ModelResourceLocation, IModel> stateModels = Loader.isModLoaded(GTValues.MODID_CTM) ?
                ReflectionHelper.getPrivateValue(ModelLoader.class, event.getModelLoader(), "stateModels", null) :
                null;

        for (Entry e : ENTRIES) {
            bakeAndRegister(event.getModelRegistry(), e.itemModelCache, e.getItemModelId(), e.modelFunction, stateModels);
            bakeAndRegister(event.getModelRegistry(), e.blockModelCache, e.getBlockModelId(), e.modelFunction, stateModels);
        }
    }

    private static void bakeAndRegister(@Nonnull IRegistry<ModelResourceLocation, IBakedModel> registry,
                                        @Nullable IModel model,
                                        @Nonnull ModelResourceLocation modelId,
                                        @Nullable UnaryOperator<IBakedModel> modelFunction,
                                        @Nullable Map<ModelResourceLocation, IModel> stateModels) {
        if (model == null) {
            // insert missing model to prevent cluttering logs with useless model loading error messages
            registry.putObject(modelId, bake(ModelLoaderRegistry.getMissingModel()));
            return;
        }
        IBakedModel baked = bake(model);
        if (modelFunction != null) {
            baked = modelFunction.apply(baked);
            if (baked == null) {
                throw new IllegalStateException("Model function returned null");
            }
        }
        registry.putObject(modelId, baked);

        if (stateModels != null) { // CTM needs the model to be present on this field to properly replace the model
            stateModels.put(modelId, model);
        }
    }

    private static IBakedModel bake(IModel model) {
        return model.bake(
                model.getDefaultState(),
                DefaultVertexFormats.ITEM,
                t -> Minecraft.getMinecraft().getTextureMapBlocks().getAtlasSprite(t.toString()));
    }

    public static final class Entry {

        private final MaterialIconType iconType;
        private final MaterialIconSet iconSet;
        private final String stateProperties;
        @Nullable
        private final UnaryOperator<IBakedModel> modelFunction;

        @Nullable
        private ModelResourceLocation blockModelId;
        @Nullable
        private ModelResourceLocation itemModelId;

        @Nullable
        private IModel itemModelCache;
        @Nullable
        private IModel blockModelCache;

        private Entry(@Nonnull MaterialIconType iconType,
                      @Nonnull MaterialIconSet iconSet,
                      @Nullable String stateProperties,
                      @Nullable UnaryOperator<IBakedModel> modelFunction) {
            this.iconType = iconType;
            this.iconSet = iconSet;
            this.stateProperties = stateProperties == null ? "" : stateProperties;
            this.modelFunction = modelFunction;
        }

        public ModelResourceLocation getBlockModelLocation() {
            return new ModelResourceLocation(iconType.getBlockstatesPath(iconSet), this.stateProperties);
        }

        public ResourceLocation getItemModelLocation() {
            ResourceLocation itemModelPath = iconType.getItemModelPath(iconSet);
            return new ResourceLocation(itemModelPath.getNamespace(), "item/" + itemModelPath.getPath());
        }

        public ModelResourceLocation getBlockModelId() {
            if (blockModelId == null) {
                this.blockModelId = new ModelResourceLocation(
                        new ResourceLocation(GTValues.MODID,
                                "material_" + iconType.name + "_" + iconSet.name),
                        "normal");
            }
            return blockModelId;
        }

        public ModelResourceLocation getItemModelId() {
            if (itemModelId == null) {
                this.itemModelId = new ModelResourceLocation(
                        new ResourceLocation(GTValues.MODID,
                                "material_" + iconType.name + "_" + iconSet.name),
                        "inventory");
            }
            return itemModelId;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Entry entry = (Entry) o;
            return iconType.equals(entry.iconType) &&
                    iconSet.equals(entry.iconSet) &&
                    Objects.equals(stateProperties, entry.stateProperties);
        }

        @Override
        public int hashCode() {
            return Objects.hash(iconType, iconSet, stateProperties);
        }

        @Override
        public String toString() {
            return "Entry{" +
                    "iconType=" + iconType +
                    ", iconSet=" + iconSet +
                    ", stateProperties='" + stateProperties + '\'' +
                    '}';
        }
    }

    public static final class EntryBuilder {
        private final MaterialIconType iconType;
        private final MaterialIconSet iconSet;
        @Nullable
        private String stateProperties = null;
        @Nullable
        private UnaryOperator<IBakedModel> modelFunction = null;

        public EntryBuilder(@Nonnull MaterialIconType iconType, @Nonnull MaterialIconSet iconSet) {
            this.iconType = iconType;
            this.iconSet = iconSet;
        }

        public EntryBuilder setStateProperties(@Nullable String stateProperties) {
            this.stateProperties = stateProperties;
            return this;
        }

        public EntryBuilder setModelFunction(@Nullable UnaryOperator<IBakedModel> modelFunction) {
            this.modelFunction = modelFunction;
            return this;
        }

        public Entry register() {
            return ENTRIES.addOrGet(new Entry(iconType, iconSet, stateProperties, modelFunction));
        }
    }
}

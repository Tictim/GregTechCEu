package gregtech.client.model;

import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonParseException;
import gregtech.api.GTValues;
import gregtech.client.model.component.ComponentModel;
import gregtech.client.model.frame.FrameModelLogicProvider;
import gregtech.client.model.pipe.CableModelLogicProvider;
import gregtech.client.model.pipe.FluidPipeModelLogicProvider;
import gregtech.client.model.pipe.ItemPipeModelLogicProvider;
import gregtech.common.pipelike.cable.Insulation;
import gregtech.common.pipelike.fluidpipe.FluidPipeType;
import gregtech.common.pipelike.itempipe.ItemPipeType;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.client.renderer.block.model.ModelBlock;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraft.client.resources.IResource;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.model.ICustomModelLoader;
import net.minecraftforge.client.model.IModel;
import net.minecraftforge.client.model.ModelLoaderRegistry;
import net.minecraftforge.client.model.ModelStateComposition;
import net.minecraftforge.common.model.IModelState;

import javax.annotation.ParametersAreNonnullByDefault;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public enum GregTechBuiltInModelLoader implements ICustomModelLoader {
    INSTANCE;

    private final Map<String, IModel> prebuiltModels = new Object2ObjectOpenHashMap<>();
    private final Pattern pattern = Pattern.compile("(?:models/)?((?:item|block)s?/)?builtin/(.+)");
    private final Matcher matcher = pattern.matcher("");

    private IResourceManager resourceManager;

    public void addPrebuiltModel(String modelLocation, IModel model) {
        if (this.prebuiltModels.putIfAbsent(Objects.requireNonNull(modelLocation), Objects.requireNonNull(model)) != null) {
            throw new IllegalStateException("Duplicated built-in model '" + modelLocation + "'");
        }
    }

    private boolean initialized;

    public void init() {
        if (initialized) return;
        else initialized = true;

        addPrebuiltModel("frame", new ComponentModel(FrameModelLogicProvider.INSTANCE));

        for (ItemPipeType itemPipeType : ItemPipeType.values()) {
            addPrebuiltModel("item_pipe/" + itemPipeType.name,
                    new ComponentModel(new ItemPipeModelLogicProvider(itemPipeType.getThickness(), itemPipeType.isRestrictive())));
        }
        for (FluidPipeType fluidPipeType : FluidPipeType.values()) {
            addPrebuiltModel("fluid_pipe/" + fluidPipeType.name,
                    new ComponentModel(new FluidPipeModelLogicProvider(fluidPipeType.getThickness())));
        }
        for (Insulation insulation : Insulation.values()) {
            addPrebuiltModel("cable/" + insulation.name,
                    new ComponentModel(new CableModelLogicProvider(insulation.getThickness(), insulation.isCable())));
        }
    }

    @Override
    public void onResourceManagerReload(IResourceManager resourceManager) {
        this.resourceManager = resourceManager;
    }

    @Override
    public boolean accepts(ResourceLocation modelLocation) {
        return !(modelLocation instanceof ModelResourceLocation) &&
                modelLocation.getNamespace().equals(GTValues.MODID) &&
                matcher.reset(modelLocation.getPath()).matches();
    }

    @Override
    public IModel loadModel(ResourceLocation modelLocation) throws Exception {
        String directory = matcher.group(1);
        if (directory == null) {
            IModel prebuiltModel = prebuiltModels.get(matcher.group(2));
            if (prebuiltModel != null) return prebuiltModel;
        }

        ResourceLocation fileLocation = new ResourceLocation(modelLocation.getNamespace(),
                "models/" + (directory == null ? "" : directory) + matcher.group(2) + ".json");

        try (IResource resource = this.resourceManager.getResource(fileLocation);
             InputStreamReader reader = new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8)) {
            ModelBlock modelBlock = ModelBlock.deserialize(reader);

            if (!modelBlock.getElements().isEmpty()) {
                throw new JsonParseException("Error on JSON built-in model '" + modelLocation + "': Changing elements are not supported");
            }
            if (!modelBlock.getOverrides().isEmpty()) {
                throw new JsonParseException("Error on JSON built-in model '" + modelLocation + "': Item overrides are not supported");
            }

            ResourceLocation parentLocation = modelBlock.getParentLocation();
            if (parentLocation == null) {
                throw new JsonParseException("Error on JSON built-in model '" + modelLocation + "': Parent model not defined");
            } else if (!this.accepts(parentLocation)) {
                throw new JsonParseException("Error on JSON built-in model '" + modelLocation + "': Parent model of a built-in JSON must be another GregTech built-in model");
            } else {
                return new Model(ModelLoaderRegistry.getModel(parentLocation)
                        .smoothLighting(modelBlock.ambientOcclusion)
                        .gui3d(modelBlock.isGui3d())
                        .retexture(ImmutableMap.copyOf(modelBlock.textures)),
                        modelBlock.getAllTransforms());
            }
        } catch (FileNotFoundException ex) {
            throw new RuntimeException("Cannot load built-in model '" + modelLocation + "', missing model file '" + fileLocation + "'");
        } catch (IOException ex) {
            throw new RuntimeException("Cannot load built-in model '" + modelLocation + "' due to I/O error", ex);
        }
    }

    private static final class Model implements IModel {

        private final IModel base;
        private final ItemCameraTransforms cameraTransforms;

        private Model(IModel base, ItemCameraTransforms cameraTransforms) {
            this.base = base;
            this.cameraTransforms = cameraTransforms;
        }

        @Override
        public Collection<ResourceLocation> getDependencies() {
            return this.base.getDependencies();
        }

        @Override
        public Collection<ResourceLocation> getTextures() {
            return this.base.getTextures();
        }

        @Override
        @SuppressWarnings("deprecation")
        public IModelState getDefaultState() {
            return new ModelStateComposition(part -> {
                if (part.isPresent() &&
                        part.get() instanceof ItemCameraTransforms.TransformType transformType &&
                        this.cameraTransforms.hasCustomTransform(transformType))
                    return this.cameraTransforms.getTransform(transformType).apply(Optional.empty());
                else return Optional.empty();
            }, this.base.getDefaultState());
        }

        @Override
        public IBakedModel bake(IModelState state, VertexFormat format, Function<ResourceLocation, TextureAtlasSprite> bakedTextureGetter) {
            return this.base.bake(state, format, bakedTextureGetter);
        }

        @Override
        public IModel smoothLighting(boolean value) {
            return new Model(this.base.smoothLighting(value), this.cameraTransforms);
        }

        @Override
        public IModel gui3d(boolean value) {
            return new Model(this.base.gui3d(value), this.cameraTransforms);
        }

        @Override
        public IModel uvlock(boolean value) {
            return new Model(this.base.uvlock(value), this.cameraTransforms);
        }

        @Override
        public IModel retexture(ImmutableMap<String, String> textures) {
            if (textures.isEmpty()) return this;
            return new Model(this.base.retexture(textures), this.cameraTransforms);
        }
    }
}

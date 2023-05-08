package gregtech.client.model;

import gregtech.api.GTValues;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.model.ICustomModelLoader;
import net.minecraftforge.client.model.IModel;

import javax.annotation.Nonnull;
import java.util.Map;
import java.util.Objects;

public enum CustomBuiltInModelLoader implements ICustomModelLoader {
    INSTANCE;

    private final Map<ResourceLocation, IModel> models = new Object2ObjectOpenHashMap<>();

    public void addBuiltInModel(@Nonnull ResourceLocation modelLocation, @Nonnull IModel model) {
        if (this.models.putIfAbsent(Objects.requireNonNull(modelLocation), Objects.requireNonNull(model)) != null) {
            throw new IllegalStateException("Duplicated built-in model '" + modelLocation + "'");
        }
    }

    private boolean initialized;

    public void init() {
        if (initialized) return;
        else initialized = true;

        addBuiltInModel(new ResourceLocation(GTValues.MODID, "builtin/frame"), new FrameModel());
    }

    @Override
    public void onResourceManagerReload(IResourceManager resources) {}

    @Override
    public boolean accepts(ResourceLocation modelLocation) {
        return models.containsKey(modelLocation);
    }

    @Override
    public IModel loadModel(ResourceLocation modelLocation) {
        return Objects.requireNonNull(models.get(modelLocation),
                "Expected model '" + modelLocation + "' to be present");
    }
}

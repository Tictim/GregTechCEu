package gregtech.client.model.connectionmultipart;

import codechicken.lib.model.loader.blockstate.CCBlockStateLoader.WeightedRandomModel;
import com.google.gson.*;
import gregtech.client.model.connectionmultipart.condition.ConnectionVariantCondition;
import gregtech.client.model.connectionmultipart.condition.ConnectionVariantConditionDeserializer;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectSet;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.client.renderer.block.model.VariantList;
import net.minecraft.client.resources.IResource;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.model.ICustomModelLoader;
import net.minecraftforge.client.model.IModel;
import net.minecraftforge.client.model.ModelLoaderRegistry;
import org.apache.commons.lang3.tuple.Pair;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public final class ConnectionVariantModelLoader implements ICustomModelLoader {

    public static final ConnectionVariantModelLoader INSTANCE = new ConnectionVariantModelLoader();

    private static final Gson GSON = new GsonBuilder()
            .setLenient()
            .registerTypeAdapter(ConnectionVariant.class, new ConnectionVariant.Deserializer())
            .registerTypeAdapter(ConnectionVariantCondition.class, new ConnectionVariantConditionDeserializer())
            .registerTypeAdapter(VariantList.class, new VariantList.Deserializer())
            .create();

    public void init() {
        ModelLoaderRegistry.registerLoader(INSTANCE);
    }

    private final ObjectSet<ResourceLocation> models = new ObjectOpenHashSet<>();
    private IResourceManager res;

    private ConnectionVariantModelLoader() {}

    public void addModel(ResourceLocation modelLocation) {
        models.add(noVariant(modelLocation));
    }

    @Override
    public void onResourceManagerReload(IResourceManager resourceManager) {
        this.res = resourceManager;
    }

    @Override
    public boolean accepts(ResourceLocation modelLocation) {
        return modelLocation instanceof ModelResourceLocation && this.models.contains(noVariant(modelLocation));
    }

    @Override
    public IModel loadModel(ResourceLocation modelLocation) throws Exception {
        IModel base = getVariantLoader().loadModel(modelLocation);

        List<ConnectionVariant> variants = new ArrayList<>();

        try (IResource r = this.res.getResource(connectionBlockstateLocation(modelLocation))) {
            JsonElement json = GSON.fromJson(
                    new InputStreamReader(r.getInputStream(), StandardCharsets.UTF_8),
                    JsonElement.class);
            if (json.isJsonObject()) {
                try {
                    variants.add(GSON.fromJson(json, ConnectionVariant.class));
                } catch (JsonParseException ex) {
                    throw new JsonParseException("Error on connection variant entry", ex);
                }
            } else if (json.isJsonArray()) {
                JsonArray arr = json.getAsJsonArray();
                for (int i = 0; i < arr.size(); i++) {
                    try {
                        variants.add(GSON.fromJson(arr.get(i), ConnectionVariant.class));
                    } catch (JsonParseException ex) {
                        throw new JsonParseException("Error on connection variant entry #" + i, ex);
                    }
                }
            } else {
                throw new JsonParseException("Expected object or array for connection blockstate");
            }
        }

        List<Pair<ConnectionVariantCondition, IModel>> variantModels = new ArrayList<>();
        for (ConnectionVariant v : variants) {
            variantModels.add(Pair.of(v.getCondition(), new WeightedRandomModel(v.getVariantList())));
        }
        return new ConnectionVariantModel(base, variantModels);
    }

    private static ResourceLocation noVariant(ResourceLocation location) {
        if (location instanceof ModelResourceLocation) {
            ModelResourceLocation mrl = (ModelResourceLocation) location;
            return new ResourceLocation(mrl.getNamespace(), mrl.getPath()); // strip off the variant
        } else {
            return location;
        }
    }

    private static ResourceLocation connectionBlockstateLocation(ResourceLocation modelLocation) {
        return new ResourceLocation(modelLocation.getNamespace(),
                "blockstates/" + modelLocation.getPath() + ".connections.json");
    }

    @Nullable
    private static ICustomModelLoader variantLoader;

    @Nonnull
    private static ICustomModelLoader getVariantLoader() {
        if (variantLoader == null) {
            try {
                return variantLoader = (ICustomModelLoader)
                        Class.forName("net.minecraftforge.client.model.ModelLoader$VariantLoader")
                                .getEnumConstants()[0];
            } catch (Exception ex) {
                throw new RuntimeException("Failed to get internal model loader instance", ex);
            }
        }
        return variantLoader;
    }

    public static final class ConnectionVariant {

        private final ConnectionVariantCondition condition;
        private final VariantList variantList;

        public ConnectionVariant(ConnectionVariantCondition condition, VariantList variantList) {
            this.condition = condition;
            this.variantList = variantList;
        }

        public ConnectionVariantCondition getCondition() {
            return condition;
        }

        public VariantList getVariantList() {
            return variantList;
        }

        public static final class Deserializer implements JsonDeserializer<ConnectionVariant> {

            @Override
            public ConnectionVariant deserialize(JsonElement json,
                                                 Type t,
                                                 JsonDeserializationContext context) throws JsonParseException {
                JsonObject o = json.getAsJsonObject();

                return new ConnectionVariant(
                        o.has("when") ?
                                context.deserialize(o.get("when"), ConnectionVariantCondition.class) :
                                ConnectionVariantCondition.always(),
                        context.deserialize(o.has("variants") ? o.get("variants") : o, VariantList.class));
            }
        }
    }
}

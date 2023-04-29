package gregtech.client.model.connectionmultipart;

import com.google.common.collect.ImmutableMap;
import gregtech.client.model.connectionmultipart.condition.ConnectionVariantCondition;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.model.IModel;
import net.minecraftforge.common.model.IModelState;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

public class ConnectionVariantModel implements IModel {

    private final IModel baseModel;
    private final List<Pair<ConnectionVariantCondition, IModel>> variants;

    public ConnectionVariantModel(IModel baseModel, List<Pair<ConnectionVariantCondition, IModel>> variants) {
        this.baseModel = baseModel;
        this.variants = variants;
    }

    @Override
    public IBakedModel bake(IModelState state,
                            VertexFormat format,
                            Function<ResourceLocation, TextureAtlasSprite> bakedTextureGetter) {
        return new ConnectionVariantBakedModel(
                this.baseModel.bake(this.baseModel.getDefaultState(), format, bakedTextureGetter),
                this.variants.stream()
                        .map(p -> Pair.of(p.getLeft(), p.getRight()
                                .bake(p.getRight().getDefaultState(), format, bakedTextureGetter)))
                        .collect(Collectors.toList()));
    }

    @Override
    public IModel retexture(ImmutableMap<String, String> textures) {
        if (textures.isEmpty()) {
            return this;
        }
        return new ConnectionVariantModel(
                this.baseModel.retexture(textures),
                this.variants.stream()
                        .map(p -> Pair.of(p.getLeft(), p.getRight().retexture(textures)))
                        .collect(Collectors.toList()));
    }

    @Override
    public Collection<ResourceLocation> getDependencies() {
        Set<ResourceLocation> deps = new ObjectOpenHashSet<>();
        deps.addAll(this.baseModel.getDependencies());
        for (Pair<ConnectionVariantCondition, IModel> pair : this.variants) {
            deps.addAll(pair.getRight().getDependencies());
        }
        return deps;
    }

    @Override
    public Collection<ResourceLocation> getTextures() {
        Set<ResourceLocation> tex = new ObjectOpenHashSet<>();
        tex.addAll(this.baseModel.getTextures());
        for (Pair<ConnectionVariantCondition, IModel> pair : this.variants) {
            tex.addAll(pair.getRight().getTextures());
        }
        return tex;
    }
}

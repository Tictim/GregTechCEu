package gregtech.client.model.special.part;

import com.google.common.collect.ImmutableMap;
import gregtech.client.model.component.ModelTextureMapping;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.model.IModelState;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.Collections;
import java.util.Objects;
import java.util.function.Function;

public final class BakedModelPart extends ModelPartEntry {

    private final IBakedModel bakedModel;

    public BakedModelPart(@Nonnull IBakedModel bakedModel) {
        this.bakedModel = Objects.requireNonNull(bakedModel, "bakedModel == null");
    }

    @Nonnull
    @Override
    public Collection<ResourceLocation> getDependencies(@Nonnull ModelTextureMapping textureMappings) {
        return Collections.emptyList();
    }

    @Nonnull
    @Override
    public Collection<ResourceLocation> getTextures(@Nonnull ModelTextureMapping textureMappings) {
        return Collections.emptyList();
    }

    @Nonnull
    @Override
    public ModelPartEntry process(@Nonnull ImmutableMap<String, String> customData) {
        return this;
    }

    @Nonnull
    @Override
    public ModelPartEntry ambientOcclusion(boolean value) {
        return this;
    }

    @Nonnull
    @Override
    public ModelPartEntry gui3d(boolean value) {
        return this;
    }

    @Nonnull
    @Override
    public ModelPartEntry uvLock(boolean value) {
        return this;
    }

    @Nonnull
    @Override
    public ModelPartEntry retexture(@Nonnull ImmutableMap<String, String> textures) {
        return this;
    }

    @Nonnull
    @Override
    public IBakedModel bake(@Nonnull IModelState state,
                            @Nonnull VertexFormat format,
                            @Nonnull Function<ResourceLocation, TextureAtlasSprite> bakedTextureGetter,
                            @Nonnull ModelTextureMapping textureMappings) {
        return this.bakedModel;
    }
}

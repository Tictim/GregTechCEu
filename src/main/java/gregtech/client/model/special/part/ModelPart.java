package gregtech.client.model.special.part;

import com.google.common.collect.ImmutableMap;
import gregtech.client.model.component.ModelTextureMapping;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.model.IModel;
import net.minecraftforge.common.model.IModelState;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.Objects;
import java.util.function.Function;

public final class ModelPart extends ModelPartEntry {

    private final IModel model;

    public ModelPart(@Nonnull IModel model) {
        this.model = Objects.requireNonNull(model, "model == null");
    }

    @Nonnull
    @Override
    public IBakedModel bake(@Nonnull IModelState state,
                            @Nonnull VertexFormat format,
                            @Nonnull Function<ResourceLocation, TextureAtlasSprite> bakedTextureGetter,
                            @Nonnull ModelTextureMapping textureMappings) {
        return this.model.bake(state, format, bakedTextureGetter);
    }

    @Nonnull
    @Override
    public Collection<ResourceLocation> getDependencies(@Nonnull ModelTextureMapping textureMappings) {
        return this.model.getDependencies();
    }

    @Nonnull
    @Override
    public Collection<ResourceLocation> getTextures(@Nonnull ModelTextureMapping textureMappings) {
        return this.model.getTextures();
    }

    @Nonnull
    @Override
    public ModelPartEntry process(@Nonnull ImmutableMap<String, String> customData) {
        IModel model = this.model.process(customData);
        return model == this.model ? this : new ModelPart(model);
    }

    @Nonnull
    @Override
    public ModelPartEntry ambientOcclusion(boolean value) {
        IModel model = this.model.smoothLighting(value);
        return model == this.model ? this : new ModelPart(model);
    }

    @Nonnull
    @Override
    public ModelPartEntry gui3d(boolean value) {
        IModel model = this.model.gui3d(value);
        return model == this.model ? this : new ModelPart(model);
    }

    @Nonnull
    @Override
    public ModelPartEntry uvLock(boolean value) {
        IModel model = this.model.uvlock(value);
        return model == this.model ? this : new ModelPart(model);
    }

    @Nonnull
    @Override
    public ModelPartEntry retexture(@Nonnull ImmutableMap<String, String> textures) {
        IModel model = this.model.retexture(textures);
        return model == this.model ? this : new ModelPart(model);
    }
}

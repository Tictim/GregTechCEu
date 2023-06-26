package gregtech.client.model.special.part;

import com.google.common.collect.ImmutableMap;
import gregtech.client.model.component.ModelTextureMapping;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.model.IModelState;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Collections;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;

public final class OptionalPart extends ModelPartEntry {

    private final Set<String> requiredTextures;
    private final ModelPartEntry optionalEntry;
    private final ModelPartEntry alternativeEntry;
    private final boolean error;

    public OptionalPart(@Nonnull Set<String> requiredTextures,
                        @Nullable ModelPartEntry optionalEntry,
                        @Nullable ModelPartEntry alternativeEntry,
                        boolean error) {
        this.requiredTextures = Objects.requireNonNull(requiredTextures, "requiredTextures == null");
        this.optionalEntry = optionalEntry;
        this.alternativeEntry = alternativeEntry;
        this.error = error;
    }

    private boolean hasRequiredTextures(@Nonnull ModelTextureMapping textureMappings) {
        for (String requiredTexture : this.requiredTextures) {
            if (!textureMappings.has(requiredTexture)) return false;
        }
        return true;
    }

    @Nonnull
    @Override
    public Collection<ResourceLocation> getDependencies(@Nonnull ModelTextureMapping textureMappings) {
        ModelPartEntry entry = hasRequiredTextures(textureMappings) ? this.optionalEntry : this.alternativeEntry;
        return entry == null ? Collections.emptyList() : entry.getDependencies(textureMappings);
    }

    @Nonnull
    @Override
    public Collection<ResourceLocation> getTextures(@Nonnull ModelTextureMapping textureMappings) {
        ModelPartEntry entry = hasRequiredTextures(textureMappings) ? this.optionalEntry : this.alternativeEntry;
        return entry == null ? Collections.emptyList() : entry.getTextures(textureMappings);
    }

    @Nonnull
    @Override
    public ModelPartEntry process(@Nonnull ImmutableMap<String, String> customData) {
        return new OptionalPart(
                this.requiredTextures,
                this.optionalEntry == null ? null : this.optionalEntry.process(customData),
                this.alternativeEntry == null ? null : this.alternativeEntry.process(customData),
                this.error);
    }

    @Nonnull
    @Override
    public ModelPartEntry ambientOcclusion(boolean value) {
        return new OptionalPart(
                this.requiredTextures,
                this.optionalEntry == null ? null : this.optionalEntry.ambientOcclusion(value),
                this.alternativeEntry == null ? null : this.alternativeEntry.ambientOcclusion(value),
                this.error);
    }

    @Nonnull
    @Override
    public ModelPartEntry gui3d(boolean value) {
        return new OptionalPart(
                this.requiredTextures,
                this.optionalEntry == null ? null : this.optionalEntry.gui3d(value),
                this.alternativeEntry == null ? null : this.alternativeEntry.gui3d(value),
                this.error);
    }

    @Nonnull
    @Override
    public ModelPartEntry uvLock(boolean value) {
        return new OptionalPart(
                this.requiredTextures,
                this.optionalEntry == null ? null : this.optionalEntry.uvLock(value),
                this.alternativeEntry == null ? null : this.alternativeEntry.uvLock(value),
                this.error);
    }

    @Nonnull
    @Override
    public ModelPartEntry retexture(@Nonnull ImmutableMap<String, String> textures) {
        return new OptionalPart(
                this.requiredTextures,
                this.optionalEntry == null ? null : this.optionalEntry.retexture(textures),
                this.alternativeEntry == null ? null : this.alternativeEntry.retexture(textures),
                this.error);
    }

    @Nullable
    @Override
    public IBakedModel bake(@Nonnull IModelState state,
                            @Nonnull VertexFormat format,
                            @Nonnull Function<ResourceLocation, TextureAtlasSprite> bakedTextureGetter,
                            @Nonnull ModelTextureMapping textureMappings) {
        boolean hasRequiredTextures = hasRequiredTextures(textureMappings);
        if (this.error && !hasRequiredTextures)
            throw new MissingTextureException(this.requiredTextures);
        ModelPartEntry entry = hasRequiredTextures ? this.optionalEntry : this.alternativeEntry;
        if (entry == null) return null;
        try {
            return entry.bake(state, format, bakedTextureGetter, textureMappings);
        } catch (MissingTextureException ex) {
            ex.addRequiredTextures(this.requiredTextures);
            throw ex;
        }
    }
}

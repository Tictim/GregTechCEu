package gregtech.client.model.special;

import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Map;
import java.util.function.Function;

public final class ModelTextureMapping {

    public static final ModelTextureMapping EMPTY = new ModelTextureMapping();

    @Nullable
    public final ModelTextureMapping parent;
    @Nullable
    public final Map<String, String> textures;

    private ModelTextureMapping() {
        this(null, null);
    }

    public ModelTextureMapping(@Nullable ModelTextureMapping parent) {
        this(parent, null);
    }

    public ModelTextureMapping(@Nullable Map<String, String> textures) {
        this(null, textures);
    }

    public ModelTextureMapping(@Nullable ModelTextureMapping parent, @Nullable Map<String, String> textures) {
        this.parent = parent;
        this.textures = textures;
    }

    @Nullable
    public ResourceLocation get(@Nullable String texture) {
        String resolved = resolve(texture);
        return resolved == null ? null : new ResourceLocation(resolved);
    }

    @Nonnull
    public ResourceLocation getOrDefault(@Nullable String texture, @Nonnull ResourceLocation fallback) {
        String resolved = resolve(texture);
        return resolved == null ? fallback : new ResourceLocation(resolved);
    }

    @Nullable
    @SideOnly(Side.CLIENT)
    public TextureAtlasSprite getTexture(@Nullable String texture,
                                         @Nonnull Function<ResourceLocation, TextureAtlasSprite> bakedTextureGetter) {
        String resolved = resolve(texture);
        return resolved == null ? null : bakedTextureGetter.apply(new ResourceLocation(resolved));
    }

    @Nonnull
    @SideOnly(Side.CLIENT)
    public TextureAtlasSprite getTextureOrMissing(@Nullable String texture,
                                                  @Nonnull Function<ResourceLocation, TextureAtlasSprite> bakedTextureGetter) {
        return getTextureOrDefault(texture, TextureMap.LOCATION_MISSING_TEXTURE, bakedTextureGetter);
    }

    @Nonnull
    @SideOnly(Side.CLIENT)
    public TextureAtlasSprite getTextureOrDefault(@Nullable String texture, @Nonnull ResourceLocation fallbackTextureLocation,
                                                  @Nonnull Function<ResourceLocation, TextureAtlasSprite> bakedTextureGetter) {
        String resolved = resolve(texture);
        return bakedTextureGetter.apply(resolved == null ? fallbackTextureLocation : new ResourceLocation(resolved));
    }

    @Nullable
    private String resolve(@Nullable String texture) {
        while (true) {
            if (texture == null) return null;
            if (!texture.startsWith("#")) return texture;
            texture = resolveInternal(texture.substring(1));
        }
    }

    @Nullable
    private String resolveInternal(@Nullable String texture) {
        if (this.textures != null) {
            String resolved = this.textures.get(texture);
            if (resolved != null) return resolved;
        }
        return this.parent != null ? this.parent.resolveInternal(texture) : null;
    }
}

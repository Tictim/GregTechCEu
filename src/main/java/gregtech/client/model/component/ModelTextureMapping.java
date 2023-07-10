package gregtech.client.model.component;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectLinkedOpenHashSet;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class ModelTextureMapping {

    public static final ModelTextureMapping EMPTY = new ModelTextureMapping();

    private final List<Map<String, String>> textures;

    @Nonnull
    private final Map<String, ResourceLocation> cache;

    private ModelTextureMapping() {
        this.textures = Collections.emptyList();
        this.cache = Collections.emptyMap();
    }

    private ModelTextureMapping(@Nonnull List<Map<String, String>> textures) {
        this.textures = textures;
        this.cache = new Object2ObjectOpenHashMap<>();
    }

    public ModelTextureMapping(@Nonnull Map<String, String> textures) {
        Objects.requireNonNull(textures, "textures == null");
        this.textures = Collections.singletonList(textures);
        this.cache = new Object2ObjectOpenHashMap<>();
    }

    public ModelTextureMapping(@Nonnull Map<String, String> textures, @Nullable ModelTextureMapping parent) {
        Objects.requireNonNull(textures, "textures == null");
        if (parent == null || parent.textures.isEmpty()) this.textures = Collections.singletonList(textures);
        else {
            this.textures = new ArrayList<>();
            this.textures.add(textures);
            this.textures.addAll(parent.textures);
        }
        this.cache = new Object2ObjectOpenHashMap<>();
    }

    public ModelTextureMapping fallback(@Nonnull Map<String, String> textures) {
        if (this.textures.isEmpty()) return new ModelTextureMapping(textures);
        List<Map<String, String>> newTextures = new ArrayList<>(this.textures);
        newTextures.add(textures);
        return new ModelTextureMapping(newTextures);
    }

    public boolean has(@Nullable String texture) {
        return get(texture) != null;
    }

    public boolean has(@Nullable ComponentTexture texture) {
        return texture != null && has(texture.textureName());
    }

    @Nullable
    public ResourceLocation get(@Nullable String texture) {
        if (texture == null) return null;
        if (!texture.startsWith("#")) return new ResourceLocation(texture);
        if (this.textures.isEmpty()) return null; // return null without modifying cache
        if (this.cache.containsKey(texture)) return this.cache.get(texture);

        Set<String> matchHistory = null;
        Loop:
        while (true) {
            String substring = texture.substring(1);
            for (Map<String, String> map : this.textures) {
                String resolved = map.get(substring);
                if (resolved == null) continue;

                if (!resolved.startsWith("#")) {
                    ResourceLocation resourceLocation = new ResourceLocation(resolved);
                    if (matchHistory != null) {
                        for (String t : matchHistory) {
                            this.cache.put(t, resourceLocation);
                        }
                    }
                    this.cache.put(texture, resourceLocation);
                    return resourceLocation;
                }
                if (matchHistory == null) {
                    matchHistory = new ObjectLinkedOpenHashSet<>();
                    matchHistory.add(texture);
                } else if (!matchHistory.add(texture)) {
                    String s = Stream.concat(matchHistory.stream(), Stream.of(texture))
                            .collect(Collectors.joining(" -> "));
                    throw new IllegalArgumentException("Circular reference detected: [" + s + "]");
                }

                texture = resolved;
                continue Loop;
            }
            if (matchHistory != null) {
                for (String t : matchHistory) {
                    this.cache.put(t, null);
                }
            }
            this.cache.put(texture, null);
            return null;
        }
    }

    @Nonnull
    public ResourceLocation getOrDefault(@Nullable String texture, @Nonnull ResourceLocation fallback) {
        ResourceLocation resolved = get(texture);
        return resolved == null ? fallback : resolved;
    }

    @Nullable
    @SideOnly(Side.CLIENT)
    public TextureAtlasSprite getTexture(@Nullable String texture,
                                         @Nonnull Function<ResourceLocation, TextureAtlasSprite> bakedTextureGetter) {
        ResourceLocation resolved = get(texture);
        return resolved == null ? null : bakedTextureGetter.apply(resolved);
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
        return bakedTextureGetter.apply(getOrDefault(texture, fallbackTextureLocation));
    }
}

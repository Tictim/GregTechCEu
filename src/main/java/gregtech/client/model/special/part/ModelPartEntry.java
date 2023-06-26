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
import java.util.function.Function;

public sealed abstract class ModelPartEntry permits ModelPart, BakedModelPart, OptionalPart {

    @Nonnull
    public abstract Collection<ResourceLocation> getDependencies(@Nonnull ModelTextureMapping textureMappings);

    @Nonnull
    public abstract Collection<ResourceLocation> getTextures(@Nonnull ModelTextureMapping textureMappings);

    @Nonnull
    public abstract ModelPartEntry process(@Nonnull ImmutableMap<String, String> customData);

    @Nonnull
    public abstract ModelPartEntry ambientOcclusion(boolean value);

    @Nonnull
    public abstract ModelPartEntry gui3d(boolean value);

    @Nonnull
    public abstract ModelPartEntry uvLock(boolean value);

    @Nonnull
    public abstract ModelPartEntry retexture(@Nonnull ImmutableMap<String, String> textures);

    @Nullable
    public abstract IBakedModel bake(@Nonnull IModelState state,
                                     @Nonnull VertexFormat format,
                                     @Nonnull Function<ResourceLocation, TextureAtlasSprite> bakedTextureGetter,
                                     @Nonnull ModelTextureMapping textureMappings);
}

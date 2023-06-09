package gregtech.client.model.special;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.model.IModel;
import net.minecraftforge.client.model.ModelLoaderRegistry;
import net.minecraftforge.common.model.IModelState;
import team.chisel.ctm.api.model.IModelCTM;
import team.chisel.ctm.api.texture.ICTMTexture;
import team.chisel.ctm.api.util.TextureInfo;
import team.chisel.ctm.client.model.ModelBakedCTM;
import team.chisel.ctm.client.texture.IMetadataSectionCTM;
import team.chisel.ctm.client.texture.render.TextureNormal;
import team.chisel.ctm.client.texture.type.TextureTypeNormal;
import team.chisel.ctm.client.util.ResourceUtil;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class SpecialConnectedModel implements IModelCTM {

    @Nullable
    public static IBakedModel bakeConnectedTextureModel(@Nonnull IModel model,
                                                        @Nonnull IModelState state,
                                                        @Nonnull VertexFormat format,
                                                        @Nonnull Function<ResourceLocation, TextureAtlasSprite> bakedTextureGetter,
                                                        @Nonnull IModel[] parts,
                                                        @Nonnull IModelLogic modelLogic,
                                                        @Nonnull TextureAtlasSprite particleTexture,
                                                        boolean ambientOcclusion,
                                                        boolean gui3d) {
        boolean hasConnectedTexture = false;

        for (ResourceLocation texture : model.getTextures()) {
            IMetadataSectionCTM meta;
            try {
                meta = ResourceUtil.getMetadata(texture);
            } catch (IOException ignored) {
                continue;
            }
            if (meta != null && meta.getType() != TextureTypeNormal.INSTANCE) {
                hasConnectedTexture = true;
                break;
            }
        }
        if (!hasConnectedTexture) return null;

        TextureGetter textureGetter = new TextureGetter(bakedTextureGetter);

        return new ModelBakedCTM(
                new SpecialConnectedModel(textureGetter.textures, textureGetter.layers),
                new SpecialBakedModel(
                        Arrays.stream(parts)
                                .map(e -> e.bake(state, format, textureGetter))
                                .collect(Collectors.toList()),
                        modelLogic,
                        particleTexture,
                        ambientOcclusion,
                        gui3d));
    }

    private final Map<String, ICTMTexture<?>> textures;
    private final byte layers;

    public SpecialConnectedModel(Map<String, ICTMTexture<?>> textures, byte layers) {
        this.textures = textures;
        this.layers = layers;
    }

    @Override
    public IModel getVanillaParent() {
        return this;
    }

    @Override
    public void load() {}

    @Override
    public Collection<ICTMTexture<?>> getChiselTextures() {
        return Collections.unmodifiableCollection(textures.values());
    }

    @Override
    public ICTMTexture<?> getTexture(String iconName) {
        return textures.get(iconName);
    }

    @Override
    public boolean canRenderInLayer(IBlockState state, BlockRenderLayer layer) {
        // sign bit is used to signify that a layer-less (vanilla) texture is present
        return (layers < 0 && state.getBlock().getRenderLayer() == layer) || (layers >> layer.ordinal() & 1) == 1;
    }

    @Nullable
    @Override
    public TextureAtlasSprite getOverrideSprite(int tintIndex) {
        return null;
    }

    @Nullable
    @Override
    public ICTMTexture<?> getOverrideTexture(int tintIndex, String sprite) {
        return null;
    }

    @Override
    public IBakedModel bake(IModelState state, VertexFormat format, Function<ResourceLocation, TextureAtlasSprite> bakedTextureGetter) {
        return ModelLoaderRegistry.getMissingModel().bake(state, format, bakedTextureGetter);
    }

    private static final class TextureGetter implements Function<ResourceLocation, TextureAtlasSprite> {

        private final Function<ResourceLocation, TextureAtlasSprite> getter;
        private final Map<String, ICTMTexture<?>> textures = new Object2ObjectOpenHashMap<>();
        private byte layers;

        private TextureGetter(Function<ResourceLocation, TextureAtlasSprite> getter) {
            this.getter = getter;
        }

        @Override
        public TextureAtlasSprite apply(ResourceLocation resourceLocation) {
            TextureAtlasSprite sprite = getter.apply(resourceLocation);
            IMetadataSectionCTM meta0 = null;
            try {
                meta0 = ResourceUtil.getMetadata(sprite);
            } catch (IOException ignored) {}
            final IMetadataSectionCTM meta = meta0;
            textures.computeIfAbsent(sprite.getIconName(), s -> {
                ICTMTexture<?> tex;
                if (meta == null) {
                    // CTM does this so it's fine I guess??
                    // noinspection ConstantConditions
                    tex = new TextureNormal(TextureTypeNormal.INSTANCE, new TextureInfo(new TextureAtlasSprite[]{sprite}, Optional.empty(), null));
                } else {
                    tex = meta.makeTexture(sprite, getter);
                }
                layers |= 1 << (tex.getLayer() == null ? 7 : tex.getLayer().ordinal());
                return tex;
            });
            return sprite;
        }
    }
}

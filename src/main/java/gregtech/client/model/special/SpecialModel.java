package gregtech.client.model.special;

import com.google.common.collect.ImmutableMap;
import gregtech.api.GTValues;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.model.IModel;
import net.minecraftforge.common.model.IModelState;
import net.minecraftforge.fml.common.Loader;

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

public class SpecialModel implements IModel {

    public final IModelLogic modelLogic;
    private final IModel[] parts;

    private boolean ambientOcclusion;
    private boolean gui3d;
    private boolean uvLock;

    private ModelTextureMapping textureMappings;

    public SpecialModel(@Nonnull IModeLogicProvider logicProvider) {
        ModelPartRegistry reg = new ModelPartRegistry();
        this.modelLogic = logicProvider.createLogic(reg);
        this.parts = reg.parts().toArray(new IModel[0]);
        Map<String, String> textureMap = logicProvider.getDefaultTextureMappings();
        this.textureMappings = textureMap.isEmpty() ? ModelTextureMapping.EMPTY : new ModelTextureMapping(textureMap);

        this.ambientOcclusion = reg.ambientOcclusion();
        this.gui3d = reg.gui3d();
        this.uvLock = reg.uvLock();
    }

    protected SpecialModel(@Nonnull SpecialModel orig) {
        this.parts = orig.parts;
        this.ambientOcclusion = orig.ambientOcclusion;
        this.gui3d = orig.gui3d;
        this.uvLock = orig.uvLock;
        this.textureMappings = orig.textureMappings;
        this.modelLogic = orig.modelLogic;
    }

    @Nonnull
    protected SpecialModel copy() {
        return new SpecialModel(this);
    }

    @Nonnull
    @Override
    public Collection<ResourceLocation> getDependencies() {
        Set<ResourceLocation> set = new ObjectOpenHashSet<>();
        for (IModel e : this.parts) {
            set.addAll(e.getDependencies());
        }
        return set;
    }

    @Nonnull
    @Override
    public Collection<ResourceLocation> getTextures() {
        Set<ResourceLocation> textures = new ObjectOpenHashSet<>();
        for (IModel e : this.parts) {
            textures.addAll(e.getTextures());
        }
        ResourceLocation particleTexture = this.textureMappings.get("#particle");
        if (particleTexture != null) {
            textures.add(particleTexture);
        }
        return textures;
    }

    @Nonnull
    @Override
    public SpecialModel process(ImmutableMap<String, String> customData) {
        return this;
    }

    @Nonnull
    @Override
    public SpecialModel smoothLighting(boolean value) {
        if (this.ambientOcclusion == value) return this;
        SpecialModel copy = copy();
        copy.ambientOcclusion = value;
        return copy;
    }

    @Nonnull
    @Override
    public SpecialModel gui3d(boolean value) {
        if (this.gui3d == value) return this;
        SpecialModel copy = copy();
        copy.gui3d = value;
        return copy;
    }

    @Nonnull
    @Override
    public SpecialModel uvlock(boolean value) {
        if (this.uvLock == value) return this;
        SpecialModel copy = copy();
        copy.uvLock = value;
        return copy;
    }

    @Nonnull
    @Override
    public SpecialModel retexture(@Nonnull ImmutableMap<String, String> textures) {
        if (textures.isEmpty()) return this;
        SpecialModel copy = copy();
        copy.retextureInternal(textures);
        return copy;
    }

    private void retextureInternal(@Nonnull ImmutableMap<String, String> textures) {
        for (int i = 0; i < this.parts.length; i++) {
            this.parts[i] = this.parts[i].retexture(textures);
        }
        this.textureMappings = new ModelTextureMapping(this.textureMappings, textures);
    }

    @Override
    public IBakedModel bake(@Nonnull IModelState state, @Nonnull VertexFormat format, @Nonnull Function<ResourceLocation, TextureAtlasSprite> bakedTextureGetter) {
        if (Loader.isModLoaded(GTValues.MODID_CTM)) {
            IBakedModel baked = SpecialConnectedModel.bakeConnectedTextureModel(this, state, format, bakedTextureGetter,
                    this.parts,
                    this.modelLogic,
                    this.textureMappings.getTextureOrMissing("#particle", bakedTextureGetter),
                    this.ambientOcclusion,
                    this.gui3d);
            if (baked != null) return baked;
        }
        return new SpecialBakedModel(
                Arrays.stream(this.parts)
                        .map(e -> e.bake(state, format, bakedTextureGetter))
                        .collect(Collectors.toList()),
                this.modelLogic,
                this.textureMappings.getTextureOrMissing("#particle", bakedTextureGetter),
                this.ambientOcclusion,
                this.gui3d);
    }
}

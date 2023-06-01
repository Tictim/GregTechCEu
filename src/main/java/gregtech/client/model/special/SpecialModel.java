package gregtech.client.model.special;

import com.google.common.collect.ImmutableMap;
import gregtech.api.GTValues;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.model.IModel;
import net.minecraftforge.common.model.IModelState;
import net.minecraftforge.fml.common.Loader;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public abstract class SpecialModel implements IModel {

    private final List<IModel> parts;

    protected boolean ambientOcclusion;
    protected boolean gui3d;
    protected boolean uvLock;

    @Nullable
    protected String particleTexture;

    private ModelTextureMapping textureMappings;

    private IModelLogic modelLogic;
    private boolean modifiable;

    public SpecialModel() {
        this.parts = new ArrayList<>();
        this.ambientOcclusion = true;
        this.gui3d = true;
        this.uvLock = false;
        this.particleTexture = "#particle";
        this.textureMappings = ModelTextureMapping.EMPTY;
    }

    protected SpecialModel(@Nonnull SpecialModel orig) {
        orig.initModelLogic();
        this.parts = orig.parts;
        this.ambientOcclusion = orig.ambientOcclusion;
        this.gui3d = orig.gui3d;
        this.uvLock = orig.uvLock;
        this.particleTexture = orig.particleTexture;
        this.textureMappings = orig.textureMappings;
        this.modelLogic = orig.modelLogic;
        this.modifiable = false;
    }

    private void initModelLogic() {
        if (this.modelLogic == null) {
            this.modifiable = true;
            this.modelLogic = buildModelLogic();
            Objects.requireNonNull(this.modelLogic, "buildModelLogic() returned null");
            this.modifiable = false;
        }
    }

    @Nonnull
    public final IModelLogic getModelLogic() {
        initModelLogic();
        return this.modelLogic;
    }

    @Nonnull
    protected abstract IModelLogic buildModelLogic();

    @Nonnull
    protected abstract SpecialModel copy();

    @CheckReturnValue
    protected final int registerPart(@Nonnull IModel model) {
        if (!this.modifiable) {
            throw new IllegalStateException("Part registration of SpecialModel must be done in #buildModelLogic() call.");
        }
        this.parts.add(Objects.requireNonNull(model, "model == null"));
        return parts.size() - 1;
    }

    @Nullable
    protected final String getParticleTexture() {
        return particleTexture;
    }

    protected final void setParticleTexture(@Nullable String particleTexture) {
        if (!this.modifiable) {
            throw new IllegalStateException("Setting particle texture of SpecialModel must be done in #buildModelLogic() call.");
        }
        this.particleTexture = particleTexture;
    }

    @Nonnull
    @Override
    public Collection<ResourceLocation> getDependencies() {
        return this.parts.stream()
                .flatMap(e -> e.getDependencies().stream())
                .collect(Collectors.toSet());
    }

    @Nonnull
    @Override
    public Collection<ResourceLocation> getTextures() {
        Set<ResourceLocation> textures = this.parts.stream()
                .flatMap(e -> e.getTextures().stream())
                .collect(Collectors.toSet());
        ResourceLocation particleTexture = this.textureMappings.get(this.particleTexture);
        if (particleTexture != null) textures.add(particleTexture);
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
        for (int i = 0; i < this.parts.size(); i++) {
            IModel retextured = this.parts.get(i).retexture(textures);
            this.parts.set(i, retextured);
        }
        this.textureMappings = new ModelTextureMapping(this.textureMappings, textures);
    }

    @Override
    public IBakedModel bake(@Nonnull IModelState state, @Nonnull VertexFormat format, @Nonnull Function<ResourceLocation, TextureAtlasSprite> bakedTextureGetter) {
        initModelLogic();
        if (Loader.isModLoaded(GTValues.MODID_CTM)) {
            IBakedModel baked = SpecialConnectedModel.bakeConnectedTextureModel(this, state, format, bakedTextureGetter,
                    this.parts,
                    this.modelLogic,
                    this.textureMappings.getTextureOrMissing(this.particleTexture, bakedTextureGetter),
                    this.ambientOcclusion,
                    this.gui3d);
            if (baked != null) return baked;
        }
        return new SpecialBakedModel(
                this.parts.stream()
                        .map(e -> e.bake(state, format, bakedTextureGetter))
                        .collect(Collectors.toList()),
                this.modelLogic,
                this.textureMappings.getTextureOrMissing(this.particleTexture, bakedTextureGetter),
                this.ambientOcclusion,
                this.gui3d);
    }
}

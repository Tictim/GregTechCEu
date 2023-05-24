package gregtech.client.model;

import com.google.common.collect.ImmutableMap;
import gregtech.common.blocks.special.ISpecialState;
import gregtech.integration.ctm.ISpecialBakedModel;
import it.unimi.dsi.fastutil.ints.IntIterator;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ItemOverrideList;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraftforge.client.model.IModel;
import net.minecraftforge.common.model.IModelState;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public abstract class SpecialModel implements IModel {

    private final List<IModel> parts = new ArrayList<>();

    protected boolean ambientOcclusion;
    protected boolean gui3d;
    protected boolean uvLock;

    @Nullable
    protected ResourceLocation particleTexture;

    public SpecialModel() {
        this.ambientOcclusion = true;
        this.gui3d = true;
        this.uvLock = false;
        this.particleTexture = null;
    }

    protected SpecialModel(@Nonnull SpecialModel orig) {
        this.parts.addAll(orig.parts);
        this.ambientOcclusion = orig.ambientOcclusion;
        this.gui3d = orig.gui3d;
        this.uvLock = orig.uvLock;
        this.particleTexture = orig.particleTexture;
    }

    protected abstract void collectModels(@Nonnull ModelCollector collector);

    @Nonnull
    protected abstract SpecialModel copy();

    @CheckReturnValue
    protected final int registerPart(@Nonnull IModel model) {
        parts.add(Objects.requireNonNull(model));
        return parts.size() - 1;
    }

    @Nonnull
    @Override
    public Collection<ResourceLocation> getDependencies() {
        return parts.stream()
                .flatMap(e -> e.getDependencies().stream())
                .collect(Collectors.toSet());
    }

    @Nonnull
    @Override
    public Collection<ResourceLocation> getTextures() {
        return parts.stream()
                .flatMap(e -> e.getTextures().stream())
                .collect(Collectors.toSet());
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
        String newParticle = textures.get("particle");
        if (newParticle != null) {
            this.particleTexture = new ResourceLocation(newParticle);
        }
    }

    @Override
    public IBakedModel bake(@Nonnull IModelState state, @Nonnull VertexFormat format, @Nonnull Function<ResourceLocation, TextureAtlasSprite> bakedTextureGetter) {
        return new Baked(
                this.parts.stream()
                        .map(e -> e.bake(state, format, bakedTextureGetter))
                        .collect(Collectors.toList()),
                this.particleTexture == null ? null : bakedTextureGetter.apply(this.particleTexture));
    }

    private final class Baked implements ISpecialBakedModel {

        private final List<IBakedModel> parts;
        private final TextureAtlasSprite particleTexture;

        Baked(@Nonnull List<IBakedModel> parts, @Nullable TextureAtlasSprite particleTexture) {
            this.parts = parts;
            this.particleTexture = particleTexture;
        }

        @Override
        public List<BakedQuad> getQuads(@Nullable IBlockState state, @Nullable EnumFacing side, long rand) {
            ModelCollector collector = new ModelCollector(state, side, rand, this.parts);
            SpecialModel.this.collectModels(collector);
            return collector.toQuads();
        }

        @Override
        public boolean isAmbientOcclusion() {
            return SpecialModel.this.ambientOcclusion;
        }

        @Override
        public boolean isAmbientOcclusion(IBlockState state) {
            return SpecialModel.this.ambientOcclusion;
        }

        @Override
        public boolean isGui3d() {
            return SpecialModel.this.gui3d;
        }

        @Override
        public boolean isBuiltInRenderer() {
            return false;
        }

        @Override
        public TextureAtlasSprite getParticleTexture() {
            return this.particleTexture;
        }

        @Override
        public ItemOverrideList getOverrides() {
            return ItemOverrideList.NONE;
        }

        @Nonnull
        @Override
        public BitSet getCacheKeys(@Nonnull ISpecialState state) {
            ModelCollector collector = new ModelCollector(state, null, 0, this.parts);
            SpecialModel.this.collectModels(collector);
            return collector.toCacheKeys();
        }
    }

    public static final class ModelCollector {

        @Nullable
        public final IBlockState state;
        @Nullable
        public final IBlockAccess world;
        @Nullable
        public final BlockPos pos;
        @Nullable
        public final EnumFacing side;
        public final long rand;

        private final List<IBakedModel> parts;

        private final IntSet includedParts = new IntOpenHashSet();

        public ModelCollector(@Nullable IBlockState state, @Nullable EnumFacing side, long rand, @Nonnull List<IBakedModel> parts) {
            this.state = state;
            if (state instanceof ISpecialState specialState) {
                this.world = specialState.getWorld();
                this.pos = specialState.getPos();
            } else {
                this.world = null;
                this.pos = null;
            }
            this.side = side;
            this.rand = rand;
            this.parts = parts;
        }

        public void includePart(int partIndex) {
            if (partIndex < 0 || partIndex >= this.parts.size()) {
                if (this.parts.isEmpty()) {
                    throw new IndexOutOfBoundsException("No parts registered");
                } else {
                    throw new IndexOutOfBoundsException("Invalid model part index; expected: 0 ~ " + (this.parts.size() - 1) + " (inclusive)");
                }
            }
            this.includedParts.add(partIndex);
        }

        @Nonnull
        private List<BakedQuad> toQuads() {
            List<BakedQuad> quads = new ArrayList<>();
            IntIterator it = this.includedParts.iterator();
            while (it.hasNext()) {
                quads.addAll(parts.get(it.nextInt()).getQuads(state, side, rand));
            }
            return quads;
        }

        @Nonnull
        private BitSet toCacheKeys() {
            BitSet bits = new BitSet(parts.size());
            for (int i = 0; i < parts.size(); i++) {
                if (includedParts.contains(i)) {
                    bits.set(i, true);
                }
            }
            return bits;
        }
    }
}

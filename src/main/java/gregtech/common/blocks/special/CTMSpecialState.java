package gregtech.common.blocks.special;

import gregtech.client.model.ISpecialBakedModel;
import gregtech.client.utils.ModelStateCache;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraftforge.common.property.IExtendedBlockState;
import net.minecraftforge.common.property.IUnlistedProperty;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import team.chisel.ctm.api.texture.ICTMTexture;
import team.chisel.ctm.api.texture.ITextureContext;
import team.chisel.ctm.api.texture.ITextureType;
import team.chisel.ctm.api.util.RenderContextList;
import team.chisel.ctm.api.util.TextureInfo;
import team.chisel.ctm.client.model.AbstractCTMBakedModel;
import team.chisel.ctm.client.state.CTMExtendedState;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

@SuppressWarnings("NullableProblems")
public class CTMSpecialState extends CTMExtendedState implements ISpecialState {

    /**
     * Counter to keep track of unique CTMSpecialState instances, for spoofing CTM's unavoidable cache strategy.
     * <p>
     * This is kind of required, since some component models are so complex that cache strategy doesn't really work;
     * they can have literally uncountable number of variations, and not only storing them in cache doesn't benefit
     * anyone, it's frankly ridiculous to ask every single component models to keep track of each possible variations
     * and assign a random number for all of them.
     * <p>
     * Each CTMSpecialState instances will increment this counter by 1 and copy the number, then it will be used as
     */
    private static int incr;

    @Nonnull
    private final IBlockState delegate;

    @Nullable
    private final IExtendedBlockState extState;

    @Nullable
    private RenderContextList ctxCache;

    @SideOnly(Side.CLIENT)
    private ModelStateCache modelStateCache;

    private final int uniqueId = ++incr;

    public CTMSpecialState(@Nonnull IBlockState state, @Nonnull IBlockAccess world, @Nonnull BlockPos pos) {
        super(state, world, pos);
        this.delegate = state;
        this.extState = delegate instanceof IExtendedBlockState ? (IExtendedBlockState) delegate : null;
    }

    public CTMSpecialState(@Nonnull IBlockState state, @Nonnull CTMExtendedState parent) {
        super(state, parent);
        this.delegate = state;
        this.extState = delegate instanceof IExtendedBlockState ? (IExtendedBlockState) delegate : null;
    }

    @Nonnull
    @Override
    public RenderContextList getContextList(@Nonnull IBlockState state, @Nonnull AbstractCTMBakedModel model) {
        if (this.ctxCache == null) {
            if (model.getParent() instanceof ISpecialBakedModel) {
                var tex = new ArrayList<>(model.getCTMTextures());
                tex.add(new CacheBypasser(this.uniqueId));
                return this.ctxCache = new RenderContextList(state, tex, getWorld(), getPos());
            } else {
                return this.ctxCache = super.getContextList(state, model);
            }
        }
        return this.ctxCache;
    }

    @Nonnull
    @Override
    @SideOnly(Side.CLIENT)
    public ModelStateCache getModelStateCache() {
        if (this.modelStateCache == null) {
            return this.modelStateCache = new ModelStateCache(this);
        }
        return this.modelStateCache;
    }

    @Nonnull
    @Override
    public <V> IExtendedBlockState withProperty(@Nullable IUnlistedProperty<V> property, @Nullable V value) {
        return this.extState != null ? new CTMSpecialState(extState.withProperty(property, value), this) : this;
    }

    @Nonnull
    @Override
    public <T extends Comparable<T>, V extends T> IBlockState withProperty(IProperty<T> property, V value) {
        return new CTMSpecialState(delegate.withProperty(property, value), this);
    }

    @Nonnull
    @Override
    public <T extends Comparable<T>> IBlockState cycleProperty(IProperty<T> property) {
        return new CTMSpecialState(delegate.cycleProperty(property), this);
    }

    private static final class CacheBypasser implements ITextureType,
            ICTMTexture<ITextureType>,
            ITextureContext {

        public final int uniqueId;

        private CacheBypasser(int uniqueId) {
            this.uniqueId = uniqueId;
        }

        @SuppressWarnings("unchecked")
        @Override
        public ICTMTexture<?> makeTexture(TextureInfo info) {
            return this;
        }

        @Override
        public ITextureContext getBlockRenderContext(IBlockState state, IBlockAccess world, BlockPos pos, ICTMTexture<?> tex) {
            return this;
        }

        @Override
        public ITextureContext getContextFromData(long data) {
            return this;
        }

        @Override
        public List<BakedQuad> transformQuad(BakedQuad quad, @Nullable ITextureContext context, int quadGoal) {
            return Collections.singletonList(quad);
        }

        @Override
        public Collection<ResourceLocation> getTextures() {
            return Collections.emptySet();
        }

        @Override
        public ITextureType getType() {
            return this;
        }

        @Nullable
        @Override
        public TextureAtlasSprite getParticle() {
            return null;
        }

        @Nullable
        @Override
        public BlockRenderLayer getLayer() {
            return null;
        }


        @Override
        public long getCompressedData() {
            return this.uniqueId;
        }

        @Override
        public int hashCode() {
            return this.uniqueId;
        }

        @Override
        public boolean equals(Object o) {
            return this == o || o instanceof CacheBypasser o2 && this.uniqueId == o2.uniqueId;
        }
    }
}

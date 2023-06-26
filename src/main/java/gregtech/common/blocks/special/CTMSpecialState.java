package gregtech.common.blocks.special;

import gregtech.client.model.component.ModelCollector;
import gregtech.integration.ctm.ConnectionCacheTextureType;
import gregtech.integration.ctm.ISpecialBakedModel;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraftforge.common.property.IExtendedBlockState;
import net.minecraftforge.common.property.IUnlistedProperty;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import team.chisel.ctm.api.texture.ICTMTexture;
import team.chisel.ctm.api.util.RenderContextList;
import team.chisel.ctm.client.model.AbstractCTMBakedModel;
import team.chisel.ctm.client.state.CTMExtendedState;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;

public class CTMSpecialState extends CTMExtendedState implements ISpecialState {

    @Nonnull
    private final IBlockState delegate;

    @Nullable
    private final IExtendedBlockState extState;

    @Nullable
    private RenderContextList ctxCache;

    @SideOnly(Side.CLIENT)
    private ModelCollector modelStateCache;

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
    public RenderContextList getContextList(IBlockState state, AbstractCTMBakedModel model) {
        if (ctxCache == null) {
            if (model.getParent() instanceof ISpecialBakedModel specialBakedModel) {
                if (this.modelStateCache == null) {
                    this.modelStateCache = specialBakedModel.collectModels(this);
                }
                BitSet cacheKeys = this.modelStateCache.toCacheKeys();
                List<ICTMTexture<?>> tex = new ArrayList<>(model.getCTMTextures());
                long[] data = cacheKeys.toLongArray();
                for (int i = 0; i < data.length; i++) {
                    tex.add(new ConnectionCacheTextureType(i, data[i]));
                }
                return ctxCache = new RenderContextList(state, tex, getWorld(), getPos());
            } else {
                return ctxCache = super.getContextList(state, model);
            }
        }
        return ctxCache;
    }

    @Nullable
    @Override
    @SideOnly(Side.CLIENT)
    public ModelCollector getModelStateCache() {
        return modelStateCache;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void setModelStateCache(@Nullable ModelCollector modelStateCache) {
        this.modelStateCache = modelStateCache;
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
}

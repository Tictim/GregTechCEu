package gregtech.common.blocks.extendedstate;

import net.minecraft.block.properties.IProperty;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraftforge.common.property.IExtendedBlockState;
import net.minecraftforge.common.property.IUnlistedProperty;
import team.chisel.ctm.client.state.CTMExtendedState;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class CTMConnectionState extends CTMExtendedState implements ConnectionState {

    @Nonnull
    private final IBlockState delegate;

    @Nullable
    private final IExtendedBlockState extState;

    private int computedFlags;
    private int connectionFlags;

    public CTMConnectionState(@Nonnull IBlockState state, IBlockAccess world, BlockPos pos) {
        super(state, world, pos);
        this.delegate = state;
        this.extState = delegate instanceof IExtendedBlockState ? (IExtendedBlockState) delegate : null;
    }

    public CTMConnectionState(@Nonnull IBlockState state, CTMExtendedState parent) {
        super(state, parent);
        this.delegate = state;
        this.extState = delegate instanceof IExtendedBlockState ? (IExtendedBlockState) delegate : null;
    }

    @Override
    public int getComputedFlags() {
        return computedFlags;
    }

    @Override
    public int getConnectionFlags() {
        return connectionFlags;
    }

    @Override
    public void setFlags(int computed, int connection) {
        this.computedFlags = computed;
        this.connectionFlags = connection;
    }

    @Override
    public <V> IExtendedBlockState withProperty(@Nullable IUnlistedProperty<V> property, @Nullable V value) {
        return this.extState != null ? new CTMConnectionState(extState.withProperty(property, value), this) : this;
    }


    @Override
    public <T extends Comparable<T>, V extends T> IBlockState withProperty(IProperty<T> property, V value) {
        return new CTMConnectionState(delegate.withProperty(property, value), this);
    }

    @Override
    public <T extends Comparable<T>> IBlockState cycleProperty(IProperty<T> property) {
        return new CTMConnectionState(delegate.cycleProperty(property), this);
    }
}

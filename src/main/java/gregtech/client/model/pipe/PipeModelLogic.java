package gregtech.client.model.pipe;

import gregtech.api.pipenet.block.BlockPipe;
import gregtech.api.pipenet.block.IPipeType;
import gregtech.api.pipenet.tile.IPipeTile;
import gregtech.client.model.component.EnumIndexedPart;
import gregtech.client.model.component.IComponentLogic;
import gregtech.client.model.component.ModelStates;
import gregtech.client.model.component.WorldContext;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.EnumFacing;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.EnumSet;

public abstract class PipeModelLogic<PipeType extends Enum<PipeType> & IPipeType<NodeDataType>, NodeDataType> implements IComponentLogic {

    protected static final int ITEM_MODEL_CONNECTION = 0b001100; // north and south

    protected final int[] base;
    protected final EnumIndexedPart<EnumFacing> closedEnd;
    protected final EnumIndexedPart<EnumFacing> openEnd;
    protected final EnumIndexedPart<EnumFacing> closedExtrusion;
    protected final EnumIndexedPart<EnumFacing> openExtrusion;

    public PipeModelLogic(@Nonnull int[] base,
                          @Nonnull EnumIndexedPart<EnumFacing> closedEnd,
                          @Nonnull EnumIndexedPart<EnumFacing> openEnd,
                          @Nonnull EnumIndexedPart<EnumFacing> closedExtrusion,
                          @Nonnull EnumIndexedPart<EnumFacing> openExtrusion) {
        this.base = base;
        this.closedEnd = closedEnd;
        this.openEnd = openEnd;
        this.closedExtrusion = closedExtrusion;
        this.openExtrusion = openExtrusion;
    }

    @Override
    public void computeStates(@Nonnull ModelStates states, @Nullable WorldContext ctx) {
        if (ctx != null) {
            IPipeTile<?, ?> te = getTileEntity(ctx);
            if (te != null && isCorrectPipeType(te.getPipeType())) {
                //noinspection unchecked
                collectPipeModels(states, ctx, (IPipeTile<PipeType, NodeDataType>) te);
                return;
            }
        }
        collectItemModels(states);
    }

    protected void collectPipeModels(@Nonnull ModelStates collector,
                                     @Nonnull WorldContext ctx,
                                     @Nonnull IPipeTile<PipeType, NodeDataType> pipeTile) {
        collector.includePart(this.base[getBlockConnection(
                pipeTile.isConnected(EnumFacing.DOWN),
                pipeTile.isConnected(EnumFacing.UP),
                pipeTile.isConnected(EnumFacing.NORTH),
                pipeTile.isConnected(EnumFacing.SOUTH),
                pipeTile.isConnected(EnumFacing.WEST),
                pipeTile.isConnected(EnumFacing.EAST))]);

        for (EnumFacing side : EnumFacing.VALUES) {
            if (!pipeTile.isConnected(side)) continue;

            if (ctx.world.getTileEntity(ctx.origin().move(side)) instanceof IPipeTile<?, ?> pipe2 &&
                    pipe2.isConnected(side.getOpposite())) {
                if (pipe2.getPipeType().getThickness() < pipeTile.getPipeType().getThickness()) {
                    collector.includePart(this.closedEnd.getPart(side));
                }
            } else {
                collector.includePart(this.openEnd.getPart(side));
            }

            if (pipeTile.getFrameMaterial() != null) {
                // TODO
            }

            // if (frameMaterial != null && !coverAtSide && BlockFrame.shouldFrameSideBeRendered(frameMaterial, world, pos, facing)) {
            //     connections |= 1 << (facing.getIndex() + 18);
            //     if (tipVisible) {
            //         connections |= 1 << (facing.getIndex() + 24);
            //     }
            // }
        }
    }

    protected void collectItemModels(@Nonnull ModelStates collector) {
        collector.includePart(this.base[ITEM_MODEL_CONNECTION]);
        collector.includePart(this.openEnd.getPart(EnumFacing.NORTH));
        collector.includePart(this.openEnd.getPart(EnumFacing.SOUTH));
    }

    @Nullable
    protected IPipeTile<?, ?> getTileEntity(@Nonnull WorldContext ctx) {
        IBlockState state = ctx.world.getBlockState(ctx.pos);
        return state.getBlock() instanceof BlockPipe<?, ?, ?> block ?
                block.getPipeTileEntity(ctx.world, ctx.pos) : null;
    }

    protected abstract boolean isCorrectPipeType(@Nonnull IPipeType<?> pipeType);

    /**
     * @param down  If the block is connected in {@link EnumFacing#DOWN} direction
     * @param up    If the block is connected in {@link EnumFacing#UP} direction
     * @param north If the block is connected in {@link EnumFacing#NORTH} direction
     * @param south If the block is connected in {@link EnumFacing#SOUTH} direction
     * @param west  If the block is connected in {@link EnumFacing#WEST} direction
     * @param east  If the block is connected in {@link EnumFacing#EAST} direction
     * @return Block connection flags
     */
    public static int getBlockConnection(boolean down, boolean up, boolean north, boolean south, boolean west, boolean east) {
        int flag = 0;
        if (down) flag |= 1;
        if (up) flag |= 2;
        if (north) flag |= 4;
        if (south) flag |= 8;
        if (west) flag |= 16;
        if (east) flag |= 32;
        return flag;
    }

    public static int getBlockConnection(@Nonnull EnumFacing... connectedSides) {
        int flag = 0;
        for (EnumFacing connectedSide : connectedSides) {
            flag |= 1 << connectedSide.ordinal();
        }
        return flag;
    }

    public static EnumSet<EnumFacing> getConnectedSides(int blockConnection) {
        EnumSet<EnumFacing> enums = EnumSet.noneOf(EnumFacing.class);
        for (EnumFacing facing : EnumFacing.VALUES) {
            if (isConnected(blockConnection, facing)) enums.add(facing);
        }
        return enums;
    }

    public static byte getSideConnection(int blockConnection, @Nonnull EnumFacing side) {
        boolean connectedToLeft;
        boolean connectedToRight;
        boolean connectedToUp;
        boolean connectedToDown;

        switch (side) {
            case DOWN -> {
                connectedToLeft = isConnected(blockConnection, EnumFacing.WEST);
                connectedToRight = isConnected(blockConnection, EnumFacing.EAST);
                connectedToUp = isConnected(blockConnection, EnumFacing.SOUTH);
                connectedToDown = isConnected(blockConnection, EnumFacing.NORTH);
            }
            case UP -> {
                connectedToLeft = isConnected(blockConnection, EnumFacing.WEST);
                connectedToRight = isConnected(blockConnection, EnumFacing.EAST);
                connectedToUp = isConnected(blockConnection, EnumFacing.NORTH);
                connectedToDown = isConnected(blockConnection, EnumFacing.SOUTH);
            }
            case NORTH -> {
                connectedToLeft = isConnected(blockConnection, EnumFacing.EAST);
                connectedToRight = isConnected(blockConnection, EnumFacing.WEST);
                connectedToUp = isConnected(blockConnection, EnumFacing.UP);
                connectedToDown = isConnected(blockConnection, EnumFacing.DOWN);
            }
            case SOUTH -> {
                connectedToLeft = isConnected(blockConnection, EnumFacing.WEST);
                connectedToRight = isConnected(blockConnection, EnumFacing.EAST);
                connectedToUp = isConnected(blockConnection, EnumFacing.UP);
                connectedToDown = isConnected(blockConnection, EnumFacing.DOWN);
            }
            case WEST -> {
                connectedToLeft = isConnected(blockConnection, EnumFacing.NORTH);
                connectedToRight = isConnected(blockConnection, EnumFacing.SOUTH);
                connectedToUp = isConnected(blockConnection, EnumFacing.UP);
                connectedToDown = isConnected(blockConnection, EnumFacing.DOWN);
            }
            case EAST -> {
                connectedToLeft = isConnected(blockConnection, EnumFacing.SOUTH);
                connectedToRight = isConnected(blockConnection, EnumFacing.NORTH);
                connectedToUp = isConnected(blockConnection, EnumFacing.UP);
                connectedToDown = isConnected(blockConnection, EnumFacing.DOWN);
            }
            default -> throw new IllegalStateException("Unreachable");
        }
        return getSideConnection(connectedToLeft, connectedToRight, connectedToUp, connectedToDown);
    }

    public static byte getSideConnection(boolean connectedToLeft, boolean connectedToRight, boolean connectedToUp, boolean connectedToDown) {
        byte flags = 0;
        if (connectedToLeft) flags |= 1;
        if (connectedToRight) flags |= 2;
        if (connectedToUp) flags |= 4;
        if (connectedToDown) flags |= 8;
        return flags;
    }

    public static boolean connectedToLeft(byte flags) {
        return (flags & 1) != 0;
    }

    public static boolean connectedToRight(byte flags) {
        return (flags & 2) != 0;
    }

    public static boolean connectedToUp(byte flags) {
        return (flags & 4) != 0;
    }

    public static boolean connectedToDown(byte flags) {
        return (flags & 8) != 0;
    }

    public static boolean connectedToAnyXPlane(byte flags) {
        return (flags & (1 | 2)) != 0;
    }

    public static boolean connectedToAnyYPlane(byte flags) {
        return (flags & (4 | 8)) != 0;
    }

    public static boolean isConnected(int connections, @Nonnull EnumFacing side) {
        return (connections & 1 << side.getIndex()) != 0;
    }
}

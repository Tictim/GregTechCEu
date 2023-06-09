package gregtech.client.model.special.pipe;

import gregtech.client.model.special.EnumIndexedPart;
import gregtech.client.model.special.IModelLogic;
import gregtech.client.model.special.ModelCollector;
import gregtech.client.model.special.WorldContext;
import net.minecraft.util.EnumFacing;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.EnumSet;

public class PipeModelLogic implements IModelLogic {

    private static final int ITEM_MODEL_CONNECTION = 0b001100; // north and south

    private final int[] models;
    private final EnumIndexedPart<EnumFacing> closedEnd;
    private final EnumIndexedPart<EnumFacing> openEnd;
    private final EnumIndexedPart<EnumFacing> extrusion;

    public PipeModelLogic(@Nonnull int[] models,
                          @Nonnull EnumIndexedPart<EnumFacing> closedEnd,
                          @Nonnull EnumIndexedPart<EnumFacing> openEnd,
                          @Nonnull EnumIndexedPart<EnumFacing> extrusion) {
        this.models = models;
        this.closedEnd = closedEnd;
        this.openEnd = openEnd;
        this.extrusion = extrusion;
    }

    @Override
    public void collectModels(@Nonnull ModelCollector collector, @Nullable WorldContext ctx) {
        // TODO
    }

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

    public static int getBlockConnection(EnumFacing... connectedSides) {
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

    public static boolean isConnected(int connections, EnumFacing side) {
        return (connections & 1 << side.getIndex()) != 0;
    }
}

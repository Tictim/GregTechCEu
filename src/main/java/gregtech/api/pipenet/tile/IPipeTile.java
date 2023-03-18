package gregtech.api.pipenet.tile;

import gregtech.api.pipenet.block.BlockPipe;
import gregtech.api.pipenet.block.IPipeType;
import gregtech.api.unification.material.Material;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.capabilities.Capability;

import javax.annotation.Nullable;
import java.util.function.Consumer;

public interface IPipeTile<PipeType extends Enum<PipeType> & IPipeType<NodeDataType>, NodeDataType> {

    World getPipeWorld();

    BlockPos getPipePos();

    default long getTickTimer() {
        return getPipeWorld().getTotalWorldTime();
    }

    BlockPipe<PipeType, NodeDataType, ?> getPipeBlock();

    void transferDataFrom(IPipeTile<PipeType, NodeDataType> sourceTile);

    int getPaintingColor();

    void setPaintingColor(int paintingColor);

    boolean isPainted();

    int getDefaultPaintingColor();

    /**
     * Get a set of bit flags denoting connection state of this pipe tile. A 'connection state' does not
     * necessarily guarantee the actual, functioning connection established between blocks; it just indicates
     * that this pipe is extended to the direction.
     * <p>
     * The connection state for certain facing is located at Nth bit, where N is the index of the facing.
     * A flag value of 1 indicates the connection is present, and a flag value of 0 indicates otherwise.
     *
     * @return A set of bit flags denoting connection state of this pipe tile
     */
    int getConnections();

    default boolean isConnected(EnumFacing side) {
        return (getConnections() & 1 << side.getIndex()) != 0;
    }

    void setConnection(EnumFacing side, boolean connected, boolean fromNeighbor);

    // if a face is blocked it will still render as connected, but it won't be able to receive stuff from that direction
    default boolean canHaveBlockedFaces() {
        return true;
    }

    /**
     * Get a set of bit flags denoting blocked state of this pipe tile. A 'blocked state' indicates
     * the connection for given side has restricted interaction.
     * <p>
     * The blocked state for certain facing is located at Nth bit, where N is the index of the facing.
     * A flag value of 1 indicates the connection is blocked, and a flag value of 0 indicates otherwise.
     *
     * @return A set of bit flags denoting blocked state of this pipe tile
     */
    int getBlockedConnections();

    default boolean isFaceBlocked(EnumFacing side) {
        return (getBlockedConnections() & (1 << side.getIndex())) != 0;
    }

    void setFaceBlocked(EnumFacing side, boolean blocked);

    /**
     * Get a set of bit flags denoting visual connection state of this pipe tile.
     * <p>
     * The content of the bit flags are as follows:
     * <ul>
     *     <li>
     *          For bit flags from 0 to 5, each bit flags indicate whether the connection is present (on 1),
     *          or not (on 1). This part is identical to {@link IPipeTile#getConnections()}.
     *     </li>
     *     <li>
     *          For bit flags from 6 to 11, each bit flags indicate whether the pipe is connected with pipes
     *          with smaller thickness.
     *     </li>
     *     <li>
     *         For bot flags from 12 to 17, each bit flags indicate whether the cover is attached to that
     *         side.
     *     </li>
     *     <li>
     *         For bot flags from 18 to 23, each bit flags indicate whether the side of frame attached should
     *         be rendered. This set of flags are empty if frames are not attached to the pipe.
     *         <p>
     *         Frames are rendered when the side is not blocked by opaque blocks or frames made of same material.
     *     </li>
     *     <li>
     *         For bot flags from 24 to 29, each bit flags indicate whether the tip of connection is extruded
     *         a bit to accommodate frames attached to the pipe.
     *     </li>
     * </ul>
     *
     * @return A set of bit flags denoting visual connection state of this pipe tile
     */
    int getVisualConnections();

    PipeType getPipeType();

    NodeDataType getNodeData();

    PipeCoverableImplementation getCoverableImplementation();

    @Nullable
    Material getFrameMaterial();

    boolean supportsTicking();

    IPipeTile<PipeType, NodeDataType> setSupportsTicking();

    boolean canPlaceCoverOnSide(EnumFacing side);

    <T> T getCapability(Capability<T> capability, EnumFacing side);

    <T> T getCapabilityInternal(Capability<T> capability, EnumFacing side);

    void notifyBlockUpdate();

    void writeCoverCustomData(int id, Consumer<PacketBuffer> writer);

    void markAsDirty();

    boolean isValidTile();

    void scheduleChunkForRenderUpdate();
}

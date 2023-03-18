package gregtech.client.renderer.pipe;

import codechicken.lib.lighting.LightMatrix;
import gregtech.api.pipenet.block.BlockPipe;
import gregtech.api.pipenet.block.IPipeType;
import gregtech.api.pipenet.tile.IPipeTile;
import gregtech.api.unification.material.Material;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class PipeRenderContext {

    private final BlockPipe<?, ?, ?> blockPipe;
    @Nullable
    private final IPipeTile<?, ?> pipeTile;
    private final IPipeType<?> pipeType;
    @Nullable
    private final Material material;

    private final float pipeThickness;
    private final int color;
    private final int connections;
    private final int blockedConnections;

    @Nullable
    private final BlockPos pos;
    @Nullable
    private final LightMatrix lightMatrix;

    public PipeRenderContext(@Nonnull BlockPipe<?, ?, ?> blockPipe, @Nullable IPipeTile<?, ?> pipeTile,
                             @Nonnull IPipeType<?> pipeType, @Nullable Material material, int connections,
                             int blockedConnections, float thickness, int color, @Nullable BlockPos pos,
                             @Nullable LightMatrix lightMatrix) {
        this.blockPipe = blockPipe;
        this.pipeTile = pipeTile;
        this.pipeType = pipeType;
        this.material = material;
        this.pos = pos;
        this.lightMatrix = lightMatrix;
        this.connections = connections & 0b111111_111111_111111_111111_111111;
        this.blockedConnections = blockedConnections & 0b111111;
        this.pipeThickness = thickness;
        this.color = color;
    }

    @Nonnull
    public BlockPipe<?, ?, ?> getBlockPipe() {
        return blockPipe;
    }

    @Nullable
    public IPipeTile<?, ?> getPipeTile() {
        return pipeTile;
    }

    @Nonnull
    public IPipeType<?> getPipeType() {
        return pipeType;
    }

    @Nullable
    public Material getMaterial() {
        return material;
    }

    public int getConnections() {
        return connections;
    }

    public int getCoverConnections() {
        return connections >> 12 & 0b111111;
    }

    public int getFrameRenderFlags() {
        return connections >> 18 & 0b111111;
    }

    public int getBlockedConnections() {
        return blockedConnections;
    }

    public float getPipeThickness() {
        return pipeThickness;
    }

    public int getColor() {
        return color;
    }

    @Nullable
    public BlockPos getPosition() {
        return pos;
    }

    @Nullable
    public LightMatrix getLightMatrix() {
        return lightMatrix;
    }

    public boolean isConnected(EnumFacing facing) {
        return (this.connections & 1 << facing.getIndex()) != 0;
    }

    public boolean isConnectedWithSmallerPipe(EnumFacing facing) {
        return (this.connections & 1 << 6 + facing.getIndex()) != 0;
    }

    public boolean shouldFrameBeRendered(EnumFacing facing) {
        return (this.connections & 1 << 18 + facing.getIndex()) != 0;
    }

    public boolean isPipeSectionExtruded(EnumFacing facing) {
        return (this.connections & 1 << 24 + facing.getIndex()) != 0;
    }

    public boolean isBlocked(EnumFacing facing) {
        return (this.blockedConnections & 1 << facing.getIndex()) != 0;
    }

    public boolean isCoverAttached(EnumFacing facing) {
        if(pipeTile==null) return false;
        return pipeTile.getCoverableImplementation().getCoverAtSide(facing) != null;
    }
}

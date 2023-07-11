package gregtech.client.model.pipe;

import gregtech.api.pipenet.block.IPipeType;
import gregtech.api.pipenet.tile.IPipeTile;
import gregtech.client.model.component.EnumIndexedPart;
import gregtech.client.model.component.ModelStates;
import gregtech.client.model.component.WorldContext;
import gregtech.common.ConfigHolder;
import gregtech.common.pipelike.optical.OpticalPipeProperties;
import gregtech.common.pipelike.optical.OpticalPipeType;
import gregtech.common.pipelike.optical.tile.TileEntityOpticalPipe;
import net.minecraft.util.EnumFacing;

import javax.annotation.Nonnull;

public class OpticalPipeModelLogic extends PipeModelLogic<OpticalPipeType, OpticalPipeProperties> {

    private final int[] baseOverlay;
    private final int[] baseOverlayActive;
    private final EnumIndexedPart<EnumFacing> extrusionOverlay;
    private final EnumIndexedPart<EnumFacing> extrusionOverlayActive;

    public OpticalPipeModelLogic(@Nonnull int[] base,
                                 @Nonnull EnumIndexedPart<EnumFacing> closedEnd,
                                 @Nonnull EnumIndexedPart<EnumFacing> openEnd,
                                 @Nonnull EnumIndexedPart<EnumFacing> closedExtrusion,
                                 @Nonnull EnumIndexedPart<EnumFacing> openExtrusion,
                                 @Nonnull int[] baseOverlay,
                                 @Nonnull int[] baseOverlayActive,
                                 @Nonnull EnumIndexedPart<EnumFacing> extrusionOverlay,
                                 @Nonnull EnumIndexedPart<EnumFacing> extrusionOverlayActive) {
        super(base, closedEnd, openEnd, closedExtrusion, openExtrusion);
        this.baseOverlay = baseOverlay;
        this.baseOverlayActive = baseOverlayActive;
        this.extrusionOverlay = extrusionOverlay;
        this.extrusionOverlayActive = extrusionOverlayActive;
    }

    @Override
    protected boolean isCorrectPipeType(@Nonnull IPipeType<?> pipeType) {
        return pipeType instanceof OpticalPipeType;
    }

    @Override
    protected void collectItemModels(@Nonnull ModelStates collector) {
        super.collectItemModels(collector);
        collector.includePart(this.baseOverlay[ITEM_MODEL_CONNECTION]);
    }

    @Override
    protected void collectPipeModels(@Nonnull ModelStates collector, @Nonnull WorldContext ctx, @Nonnull IPipeTile<OpticalPipeType, OpticalPipeProperties> pipeTile) {
        super.collectPipeModels(collector, ctx, pipeTile);
        boolean active = !ConfigHolder.client.preventAnimatedCables &&
                pipeTile instanceof TileEntityOpticalPipe opticalPipe &&
                opticalPipe.isActive();

        int[] baseOverlay = active ? this.baseOverlayActive : this.baseOverlay;

        collector.includePart(baseOverlay[getBlockConnection(
                pipeTile.isConnected(EnumFacing.DOWN),
                pipeTile.isConnected(EnumFacing.UP),
                pipeTile.isConnected(EnumFacing.NORTH),
                pipeTile.isConnected(EnumFacing.SOUTH),
                pipeTile.isConnected(EnumFacing.WEST),
                pipeTile.isConnected(EnumFacing.EAST))]);

        if (pipeTile.getFrameMaterial() != null) {
            EnumIndexedPart<EnumFacing> extrusionOverlay = active ? this.extrusionOverlayActive : this.extrusionOverlay;

            for (EnumFacing side : EnumFacing.VALUES) {
                if (isSideExtruded(ctx, pipeTile, side)) {
                    collector.includePart(extrusionOverlay.getPart(side));
                }
            }
        }
    }
}

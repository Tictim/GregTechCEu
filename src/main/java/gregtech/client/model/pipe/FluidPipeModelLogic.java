package gregtech.client.model.pipe;

import gregtech.api.pipenet.block.IPipeType;
import gregtech.api.pipenet.tile.IPipeTile;
import gregtech.api.unification.material.properties.FluidPipeProperties;
import gregtech.client.model.component.EnumIndexedPart;
import gregtech.client.model.component.ModelStates;
import gregtech.client.model.component.WorldContext;
import gregtech.common.pipelike.fluidpipe.FluidPipeType;
import net.minecraft.util.EnumFacing;

import javax.annotation.Nonnull;

public class FluidPipeModelLogic extends PipeModelLogic<FluidPipeType, FluidPipeProperties> {

    private final EnumIndexedPart<EnumFacing> restrictedSide;
    private final EnumIndexedPart<EnumFacing> restrictedExtrusion;

    public FluidPipeModelLogic(@Nonnull int[] base,
                               @Nonnull EnumIndexedPart<EnumFacing> closedEnd,
                               @Nonnull EnumIndexedPart<EnumFacing> openEnd,
                               @Nonnull EnumIndexedPart<EnumFacing> closedExtrusion,
                               @Nonnull EnumIndexedPart<EnumFacing> openExtrusion,
                               @Nonnull EnumIndexedPart<EnumFacing> restrictedSide,
                               @Nonnull EnumIndexedPart<EnumFacing> restrictedExtrusion) {
        super(base, closedEnd, openEnd, closedExtrusion, openExtrusion);
        this.restrictedSide = restrictedSide;
        this.restrictedExtrusion = restrictedExtrusion;
    }

    @Override
    protected boolean isCorrectPipeType(@Nonnull IPipeType<?> pipeType) {
        return pipeType instanceof FluidPipeType;
    }

    @Override
    protected void collectPipeModels(@Nonnull ModelStates collector, @Nonnull WorldContext ctx, @Nonnull IPipeTile<FluidPipeType, FluidPipeProperties> pipeTile) {
        super.collectPipeModels(collector, ctx, pipeTile);
        for (EnumFacing facing : EnumFacing.VALUES) {
            if (pipeTile.isConnected(facing) && pipeTile.isFaceBlocked(facing)) {
                collector.includePart(this.restrictedSide.getPart(facing));
                if (pipeTile.getFrameMaterial() != null &&
                        pipeTile.getCoverableImplementation().getCoverAtSide(facing) == null) {
                    collector.includePart(this.restrictedExtrusion.getPart(facing));
                }
            }
        }
    }
}

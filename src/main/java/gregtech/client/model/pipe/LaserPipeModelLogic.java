package gregtech.client.model.pipe;

import gregtech.api.pipenet.block.IPipeType;
import gregtech.client.model.component.EnumIndexedPart;
import gregtech.common.pipelike.laser.LaserPipeProperties;
import gregtech.common.pipelike.laser.LaserPipeType;
import net.minecraft.util.EnumFacing;

import javax.annotation.Nonnull;

public class LaserPipeModelLogic extends PipeModelLogic<LaserPipeType, LaserPipeProperties> {

    public LaserPipeModelLogic(@Nonnull int[] base,
                               @Nonnull EnumIndexedPart<EnumFacing> closedEnd,
                               @Nonnull EnumIndexedPart<EnumFacing> openEnd,
                               @Nonnull EnumIndexedPart<EnumFacing> closedExtrusion,
                               @Nonnull EnumIndexedPart<EnumFacing> openExtrusion) {
        super(base, closedEnd, openEnd, closedExtrusion, openExtrusion);
    }

    @Override
    protected boolean isCorrectPipeType(@Nonnull IPipeType<?> pipeType) {
        return pipeType instanceof LaserPipeType;
    }
}

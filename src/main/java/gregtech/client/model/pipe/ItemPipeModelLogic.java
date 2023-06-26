package gregtech.client.model.pipe;

import gregtech.api.pipenet.block.IPipeType;
import gregtech.api.pipenet.tile.IPipeTile;
import gregtech.api.unification.material.properties.ItemPipeProperties;
import gregtech.client.model.component.EnumIndexedPart;
import gregtech.client.model.component.ModelCollector;
import gregtech.client.model.component.WorldContext;
import gregtech.common.pipelike.itempipe.ItemPipeType;
import net.minecraft.util.EnumFacing;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class ItemPipeModelLogic extends PipeModelLogic<ItemPipeType, ItemPipeProperties> {

    @Nullable
    private final int[] restrictionOverlay;

    public ItemPipeModelLogic(@Nonnull int[] base,
                              @Nonnull EnumIndexedPart<EnumFacing> closedEnd,
                              @Nonnull EnumIndexedPart<EnumFacing> openEnd,
                              @Nonnull EnumIndexedPart<EnumFacing> closedExtrusion,
                              @Nonnull EnumIndexedPart<EnumFacing> openExtrusion,
                              @Nullable int[] restrictionOverlay) {
        super(base, closedEnd, openEnd, closedExtrusion, openExtrusion);
        this.restrictionOverlay = restrictionOverlay;
    }

    @Override
    protected void collectPipeModels(@Nonnull ModelCollector collector,
                                     @Nonnull WorldContext ctx,
                                     @Nonnull IPipeTile<ItemPipeType, ItemPipeProperties> pipeTile) {

    }

    @Override
    protected boolean isCorrectPipeType(@Nonnull IPipeType<?> pipeType) {
        return pipeType instanceof ItemPipeType;
    }
}

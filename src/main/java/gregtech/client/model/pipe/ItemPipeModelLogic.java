package gregtech.client.model.pipe;

import gregtech.api.pipenet.block.IPipeType;
import gregtech.api.unification.material.properties.ItemPipeProperties;
import gregtech.client.model.component.EnumIndexedPart;
import gregtech.common.pipelike.itempipe.ItemPipeType;
import net.minecraft.util.EnumFacing;

import javax.annotation.Nonnull;

public class ItemPipeModelLogic extends PipeModelLogic<ItemPipeType, ItemPipeProperties> {

    public ItemPipeModelLogic(@Nonnull int[] base,
                              @Nonnull EnumIndexedPart<EnumFacing> closedEnd,
                              @Nonnull EnumIndexedPart<EnumFacing> openEnd,
                              @Nonnull EnumIndexedPart<EnumFacing> closedExtrusion,
                              @Nonnull EnumIndexedPart<EnumFacing> openExtrusion) {
        super(base, closedEnd, openEnd, closedExtrusion, openExtrusion);
    }

    @Override
    protected boolean isCorrectPipeType(@Nonnull IPipeType<?> pipeType) {
        return pipeType instanceof ItemPipeType;
    }
}

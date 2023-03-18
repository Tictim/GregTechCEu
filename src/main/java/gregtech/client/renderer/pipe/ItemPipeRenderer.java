package gregtech.client.renderer.pipe;

import gregtech.api.GTValues;
import gregtech.api.pipenet.block.IPipeType;
import gregtech.api.unification.material.Material;
import gregtech.common.pipelike.itempipe.ItemPipeType;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.util.ResourceLocation;

import javax.annotation.Nullable;
import java.util.EnumMap;

public class ItemPipeRenderer extends PipeRenderer {

    public static final ItemPipeRenderer INSTANCE = new ItemPipeRenderer();

    private TextureAtlasSprite restrictiveOverlay;
    private TextureAtlasSprite pipeSide;

    private final EnumMap<ItemPipeType, TextureAtlasSprite> openPipeTextures = new EnumMap<>(ItemPipeType.class);

    private final EnumMap<ItemPipeType, TextureAtlasSprite> dynamicRestrictiveOverlays = new EnumMap<>(ItemPipeType.class);
    private final EnumMap<ItemPipeType, TextureAtlasSprite> dynamicPipes = new EnumMap<>(ItemPipeType.class);

    private ItemPipeRenderer() {
        super("gt_item_pipe", new ResourceLocation(GTValues.MODID, "item_pipe"));
    }

    @Override
    protected void registerPipeTextures(TextureMap map) {
        this.restrictiveOverlay = texture(map, GTValues.MODID, "blocks/pipe/pipe_restrictive");
        this.pipeSide = texture(map, GTValues.MODID, "blocks/pipe/pipe_side");

        initialize(map, this.openPipeTextures, "blocks/pipe/pipe_%s_in", false);

        initialize(map, this.dynamicRestrictiveOverlays, "blocks/pipe/dynamic/pipe_%s_restrictive", true);
        initialize(map, this.dynamicPipes, "blocks/pipe/dynamic/pipe_%s", true);
    }

    private static void initialize(TextureMap map, EnumMap<ItemPipeType, TextureAtlasSprite> collection,
                                   String location, boolean optional) {
        TextureAtlasSprite small = texture(map, GTValues.MODID, String.format(location, "small"), optional);
        collection.put(ItemPipeType.SMALL, small);
        collection.put(ItemPipeType.RESTRICTIVE_SMALL, small);
        TextureAtlasSprite normal = texture(map, GTValues.MODID, String.format(location, "normal"), optional);
        collection.put(ItemPipeType.NORMAL, normal);
        collection.put(ItemPipeType.RESTRICTIVE_NORMAL, normal);
        TextureAtlasSprite large = texture(map, GTValues.MODID, String.format(location, "large"), optional);
        collection.put(ItemPipeType.LARGE, large);
        collection.put(ItemPipeType.RESTRICTIVE_LARGE, large);
        TextureAtlasSprite huge = texture(map, GTValues.MODID, String.format(location, "huge"), optional);
        collection.put(ItemPipeType.HUGE, huge);
        collection.put(ItemPipeType.RESTRICTIVE_HUGE, huge);
    }

    @Override
    protected void buildPipelines(PipeRenderContext context, CachedPipeline openFace, CachedPipeline side) {
        if (context.getPipeType() instanceof ItemPipeType) {
            ItemPipeType pipeType = (ItemPipeType) context.getPipeType();
            openFace.addSprite(openPipeTextures.get(pipeType));
            side.addSideSprite(dynamicPipes.get(pipeType), pipeSide);
            if (pipeType.isRestrictive()) {
                side.addSideSprite(false, dynamicRestrictiveOverlays.get(pipeType), restrictiveOverlay);
            }
        }
    }

    @Override
    public TextureAtlasSprite getParticleTexture(IPipeType<?> pipeType, @Nullable Material material) {
        return pipeSide;
    }
}

package gregtech.client.renderer.pipe;

import gregtech.api.GTValues;
import gregtech.api.pipenet.block.IPipeType;
import gregtech.api.recipes.ModHandler;
import gregtech.api.unification.material.Material;
import gregtech.api.util.GTUtility;
import gregtech.common.pipelike.fluidpipe.FluidPipeType;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureMap;

import javax.annotation.Nullable;
import java.util.EnumMap;

public class FluidPipeRenderer extends PipeRenderer {

    public static final FluidPipeRenderer INSTANCE = new FluidPipeRenderer();

    private TextureAtlasSprite pipeSide;
    private TextureAtlasSprite pipeSideWood;

    private final EnumMap<FluidPipeType, TextureAtlasSprite> openFaces = new EnumMap<>(FluidPipeType.class);
    private final EnumMap<FluidPipeType, TextureAtlasSprite> openFacesWood = new EnumMap<>(FluidPipeType.class);

    private final EnumMap<FluidPipeType, TextureAtlasSprite> dynamicPipes = new EnumMap<>(FluidPipeType.class);
    private final EnumMap<FluidPipeType, TextureAtlasSprite> dynamicPipesWood = new EnumMap<>(FluidPipeType.class);

    private FluidPipeRenderer() {
        super("gt_fluid_pipe", GTUtility.gregtechId("fluid_pipe"));
    }

    @Override
    protected void registerPipeTextures(TextureMap map) {
        this.pipeSide = texture(map, GTValues.MODID, "blocks/pipe/pipe_side");
        this.pipeSideWood = texture(map, GTValues.MODID, "blocks/pipe/pipe_side_wood");

        initialize(map, this.openFaces, "blocks/pipe/pipe_%s_in", false,
                FluidPipeType.values());
        initialize(map, this.openFacesWood, "blocks/pipe/pipe_%s_in_wood", false,
                FluidPipeType.SMALL, FluidPipeType.NORMAL, FluidPipeType.LARGE);

        initialize(map, this.dynamicPipes, "blocks/pipe/dynamic/pipe_%s", true,
                FluidPipeType.values());
        initialize(map, this.dynamicPipesWood, "blocks/pipe/dynamic/pipe_%s_wood", true,
                FluidPipeType.SMALL, FluidPipeType.NORMAL, FluidPipeType.LARGE);
    }

    private static void initialize(TextureMap map, EnumMap<FluidPipeType, TextureAtlasSprite> collection,
                                   String location, boolean optional, FluidPipeType... types) {
        for (FluidPipeType type : types) {
            collection.put(type, texture(map, GTValues.MODID, String.format(location, type.name), optional));
        }
    }

    @Override
    protected void buildPipelines(PipeRenderContext context, CachedPipeline openFace, CachedPipeline side) {
        IPipeType<?> pipeType = context.getPipeType();
        if (pipeType instanceof FluidPipeType) {
            if (ModHandler.isMaterialWood(context.getMaterial())) {
                TextureAtlasSprite sprite = openFacesWood.get(pipeType);
                openFace.addSprite(sprite != null ? sprite : openFaces.get(pipeType));
                side.addSideSprite(dynamicPipesWood.get(pipeType), pipeSideWood);
            } else {
                openFace.addSprite(openFaces.get(pipeType));
                side.addSideSprite(dynamicPipes.get(pipeType), pipeSide);
            }
        }
    }

    @Override
    public TextureAtlasSprite getParticleTexture(IPipeType<?> pipeType, @Nullable Material material) {
        return ModHandler.isMaterialWood(material) ? pipeSideWood : pipeSide;
    }
}

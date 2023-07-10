package gregtech.client.renderer.pipe;

import gregtech.api.GTValues;
import gregtech.api.pipenet.block.IPipeType;
import gregtech.api.unification.material.Material;
import gregtech.api.util.GTUtility;
import gregtech.common.ConfigHolder;
import gregtech.common.pipelike.optical.OpticalPipeType;
import gregtech.common.pipelike.optical.tile.TileEntityOpticalPipe;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.util.ResourceLocation;

import javax.annotation.Nullable;
import java.util.EnumMap;

public final class OpticalPipeRenderer extends PipeRenderer {

    public static final OpticalPipeRenderer INSTANCE = new OpticalPipeRenderer();
    private final EnumMap<OpticalPipeType, TextureAtlasSprite> pipeTextures = new EnumMap<>(OpticalPipeType.class);

    public static TextureAtlasSprite opticalPipeIn;
    public static TextureAtlasSprite opticalPipeSide;
    public static TextureAtlasSprite opticalPipeSideOverlay;
    public static TextureAtlasSprite opticalPipeSideOverlayActive;

    private OpticalPipeRenderer() {
        super("gt_optical_pipe", GTUtility.gregtechId("optical_pipe"));
    }

    @Override
    public void registerIcons(TextureMap map) {
    }

    @Override
    protected void registerPipeTextures(TextureMap map) {
        opticalPipeIn = map.registerSprite(new ResourceLocation(GTValues.MODID, "blocks/pipe/pipe_optical_in"));
        opticalPipeSide = map.registerSprite(new ResourceLocation(GTValues.MODID, "blocks/pipe/pipe_optical_side"));
        opticalPipeSideOverlay = map.registerSprite(new ResourceLocation(GTValues.MODID, "blocks/pipe/pipe_optical_side_overlay"));
        opticalPipeSideOverlayActive = map.registerSprite(new ResourceLocation(GTValues.MODID, "blocks/pipe/pipe_optical_side_overlay_active"));

        pipeTextures.put(OpticalPipeType.NORMAL, opticalPipeIn);
    }

    @Override
    protected void buildPipelines(PipeRenderContext context, CachedPipeline openFace, CachedPipeline side) {
        if (context.getPipeType() instanceof OpticalPipeType pipeType) {
            openFace.addSprite(pipeTextures.get(pipeType));
            side.addSprite(opticalPipeSide);

            if (ConfigHolder.client.preventAnimatedCables) {
                side.addSprite(opticalPipeSideOverlay);
            } else if (context.getPipeTile() instanceof TileEntityOpticalPipe opticalPipe && opticalPipe.isActive()) {
                side.addSprite(opticalPipeSideOverlayActive);
            } else {
                side.addSprite(opticalPipeSideOverlay);
            }
        }
    }

    @Override
    public TextureAtlasSprite getParticleTexture(IPipeType<?> pipeType, @Nullable Material material) {
        return opticalPipeSide;
    }
}

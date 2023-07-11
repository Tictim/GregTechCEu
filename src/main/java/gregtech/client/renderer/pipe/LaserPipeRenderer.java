package gregtech.client.renderer.pipe;

import codechicken.lib.render.CCRenderState;
import gregtech.api.GTValues;
import gregtech.api.pipenet.block.IPipeType;
import gregtech.api.unification.material.Material;
import gregtech.api.util.GTUtility;
import gregtech.client.utils.BloomEffectUtil;
import gregtech.common.pipelike.laser.LaserPipeType;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.ResourceLocation;
import org.jetbrains.annotations.Nullable;

public class LaserPipeRenderer extends PipeRenderer {

    public static final LaserPipeRenderer INSTANCE = new LaserPipeRenderer();

    private TextureAtlasSprite laserPipeIn;
    private TextureAtlasSprite laserPipeSide;
    private TextureAtlasSprite laserPipeOverlay;
    private TextureAtlasSprite laserPipeOverlayEmissive;

    public LaserPipeRenderer() {
        super("gt_laser_pipe", GTUtility.gregtechId("laser_pipe"));
    }

    @Override
    protected void registerPipeTextures(TextureMap map) {
        this.laserPipeSide = map.registerSprite(new ResourceLocation(GTValues.MODID, "blocks/pipe/pipe_laser_side"));
        this.laserPipeIn = map.registerSprite(new ResourceLocation(GTValues.MODID, "blocks/pipe/pipe_laser_in"));
        this.laserPipeOverlay = map.registerSprite(new ResourceLocation(GTValues.MODID, "blocks/pipe/pipe_laser_side_overlay"));
        this.laserPipeOverlayEmissive = map.registerSprite(new ResourceLocation(GTValues.MODID, "blocks/pipe/pipe_laser_side_overlay_emissive"));
    }

    @Override
    protected void buildPipelines(PipeRenderContext context, CachedPipeline openFace, CachedPipeline side) {
        if (context.getPipeType() instanceof LaserPipeType) {
            openFace.addSprite(laserPipeIn);
            side.addSprite(laserPipeSide);

            if (context.getPipeTile() != null && context.getPipeTile().isPainted()) {
                side.addSprite(laserPipeOverlay);
            }
        }
    }

    @Override
    protected void renderOtherLayers(BlockRenderLayer layer, CCRenderState renderState, PipeRenderContext renderContext) {
        // if (!ConfigHolder.client.preventAnimatedCables &&
        //         layer == BloomEffectUtil.getRealBloomLayer() &&
        //         renderContext.getPipeTile() instanceof TileEntityLaserPipe laserPipe && laserPipe.isActive() &&
        //         (renderContext.getConnections() & 0b111111) != 0) {
        //     Cuboid6 innerCuboid = BlockPipe.getSideBox(null, renderContext.getPipeThickness());
        //     for (EnumFacing side : EnumFacing.VALUES) {
        //         if ((renderContext.getConnections() & (1 << side.getIndex())) == 0) {
        //             int oppositeIndex = side.getOpposite().getIndex();
        //             if ((renderContext.getConnections() & (1 << oppositeIndex)) <= 0 || (renderContext.getConnections() & 0b111111 & ~(1 << oppositeIndex)) != 0) {
        //                 // render pipe side
        //                 IVertexOperation[] ops = renderContext.getBaseVertexOperation();
        //                 ops = ArrayUtils.addAll(ops, new IconTransformation(laserPipeOverlayEmissive));
        //                 renderFace(renderState, ops, side, innerCuboid);
        //             }
        //         } else {
        //             // render connection cuboid
        //             Cuboid6 sideCuboid = BlockPipe.getSideBox(side, renderContext.getPipeThickness());
        //             for (EnumFacing connectionSide : EnumFacing.VALUES) {
        //                 if (connectionSide.getAxis() != side.getAxis()) {
        //                     // render side textures
        //                     IVertexOperation[] ops = renderContext.getBaseVertexOperation();
        //                     ops = ArrayUtils.addAll(ops, new IconTransformation(laserPipeOverlayEmissive));
        //                     renderFace(renderState, ops, connectionSide, sideCuboid);
        //                 }
        //             }
        //         }
        //     }
        // }
    }

    @Override
    protected boolean canRenderInLayer(BlockRenderLayer layer) {
        return super.canRenderInLayer(layer) || layer == BloomEffectUtil.getRealBloomLayer();
    }

    @Override
    public TextureAtlasSprite getParticleTexture(IPipeType<?> pipeType, @Nullable Material material) {
        return laserPipeSide;
    }
}

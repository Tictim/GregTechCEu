package gregtech.client.renderer.pipe;

import codechicken.lib.render.BlockRenderer;
import codechicken.lib.render.CCQuad;
import codechicken.lib.render.CCRenderState;
import codechicken.lib.render.block.BlockRenderingRegistry;
import codechicken.lib.render.block.ICCBlockRenderer;
import codechicken.lib.render.item.IItemRenderer;
import codechicken.lib.render.pipeline.ColourMultiplier;
import codechicken.lib.render.pipeline.IVertexOperation;
import codechicken.lib.texture.TextureUtils;
import codechicken.lib.texture.TextureUtils.IIconRegister;
import codechicken.lib.util.TransformUtils;
import codechicken.lib.vec.*;
import codechicken.lib.vec.uv.IconTransformation;
import codechicken.lib.vec.uv.UV;
import gregtech.api.GTValues;
import gregtech.api.cover.CoverBehavior;
import gregtech.api.gui.resources.ResourceHelper;
import gregtech.api.pipenet.block.BlockPipe;
import gregtech.api.pipenet.block.IPipeType;
import gregtech.api.pipenet.block.ItemBlockPipe;
import gregtech.api.pipenet.block.material.BlockMaterialPipe;
import gregtech.api.pipenet.block.material.TileEntityMaterialPipeBase;
import gregtech.api.pipenet.tile.IPipeTile;
import gregtech.api.pipenet.tile.PipeCoverableImplementation;
import gregtech.api.unification.material.Material;
import gregtech.api.util.GTLog;
import gregtech.api.util.GTUtility;
import gregtech.api.util.ModCompatibility;
import gregtech.client.renderer.CubeRendererState;
import gregtech.client.renderer.texture.Textures;
import gregtech.client.utils.AdvCCRSConsumer;
import gregtech.client.utils.PipelineUtil;
import gregtech.common.blocks.BlockFrame;
import gregtech.common.blocks.MetaBlocks;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms.TransformType;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.item.ItemStack;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumBlockRenderType;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumFacing.AxisDirection;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.IBlockAccess;
import net.minecraftforge.client.MinecraftForgeClient;
import net.minecraftforge.client.event.ModelBakeEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.model.IModelState;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.apache.commons.lang3.tuple.Pair;
import org.lwjgl.opengl.GL11;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@SideOnly(Side.CLIENT)
public abstract class PipeRenderer implements ICCBlockRenderer, IItemRenderer, IIconRegister {

    private static final float PIPE_EXTRUSION_SIZE = 1 / 16f;
    private static final float PIPE_EXTRUSION_OFFSET = 2 / 16f;
    private static final ThreadLocal<BlockRenderer.BlockFace> BLOCK_FACE = ThreadLocal.withInitial(BlockRenderer.BlockFace::new);

    private final String name;
    private final ModelResourceLocation modelLocation;

    private EnumBlockRenderType blockRenderType;

    @Nullable
    protected TextureAtlasSprite blockedOverlay;

    public PipeRenderer(String name, ModelResourceLocation modelLocation) {
        this.name = name;
        this.modelLocation = modelLocation;
    }

    public PipeRenderer(String name, ResourceLocation modelLocation) {
        this(name, new ModelResourceLocation(modelLocation, "normal"));
    }

    public void preInit() {
        this.blockRenderType = BlockRenderingRegistry.createRenderType(name);
        BlockRenderingRegistry.registerRenderer(this.blockRenderType, this);
        MinecraftForge.EVENT_BUS.register(this);
        Textures.iconRegisters.add(this);
    }

    public ModelResourceLocation getModelLocation() {
        return this.modelLocation;
    }

    public EnumBlockRenderType getBlockRenderType() {
        return this.blockRenderType;
    }

    @Override
    public void registerIcons(TextureMap map) {
        registerPipeTextures(map);
        registerBlockedOverlay(map);
    }

    protected abstract void registerPipeTextures(TextureMap map);

    protected void registerBlockedOverlay(TextureMap map) {
        this.blockedOverlay = texture(map, GTValues.MODID, "blocks/pipe/pipe_blocked");
    }

    @SubscribeEvent
    public void onModelsBake(ModelBakeEvent event) {
        event.getModelRegistry().putObject(modelLocation, this);
    }

    protected abstract void buildPipelines(PipeRenderContext context, CachedPipeline openFace, CachedPipeline side);

    protected void buildBlockedOverlayPipeline(PipeRenderContext context, CachedPipeline blockedOverlay) {
        if (this.blockedOverlay != null) {
            blockedOverlay.addSprite(false, this.blockedOverlay);
        }
    }

    @Override
    public void renderItem(ItemStack rawItemStack, TransformType transformType) {
        ItemStack stack = ModCompatibility.getRealItemStack(rawItemStack);
        if (!(stack.getItem() instanceof ItemBlockPipe item)) {
            return;
        }
        CCRenderState renderState = CCRenderState.instance();
        GlStateManager.enableBlend();
        renderState.reset();
        renderState.startDrawing(GL11.GL_QUADS, DefaultVertexFormats.ITEM);
        BlockPipe<?, ?, ?> blockFluidPipe = (BlockPipe<?, ?, ?>) item.getBlock();
        IPipeType<?> pipeType = blockFluidPipe.getItemPipeType(stack);
        if (pipeType != null) {
            Material material = blockFluidPipe instanceof BlockMaterialPipe blockMaterialPipe ? blockMaterialPipe.getItemMaterial(stack) : null;
            PipeRenderContext renderContext = new PipeRenderContext(blockFluidPipe, null, pipeType, material,
                    0b1100, // North and South connection (index 2 & 3)
                    0,
                    pipeType.getThickness(),
                    GTUtility.convertRGBtoOpaqueRGBA_CL(material == null ? 0xFFFFFF : material.getMaterialRGB()),
                    null, null);
            renderPipeBlock(renderState, renderContext);
        }
        renderState.draw();
        GlStateManager.disableBlend();
    }

    @Override
    public boolean renderBlock(IBlockAccess world, BlockPos pos, IBlockState state, BufferBuilder buffer) {
        BlockPipe<?, ?, ?> blockPipe = (BlockPipe<?, ?, ?>) state.getBlock();
        IPipeTile<?, ?> pipeTile = blockPipe.getPipeTileEntity(world, pos);

        if (pipeTile == null) {
            return false;
        }

        IPipeType<?> pipeType = pipeTile.getPipeType();

        if (pipeType != null) {
            CCRenderState renderState = CCRenderState.instance();
            renderState.reset();
            renderState.bind(buffer);
            renderState.setBrightness(world, pos);

            BlockRenderLayer renderLayer = MinecraftForgeClient.getRenderLayer();
            boolean[] sideMask = new boolean[EnumFacing.VALUES.length];
            for (EnumFacing side : EnumFacing.VALUES) {
                sideMask[side.getIndex()] = state.shouldSideBeRendered(world, pos, side);
            }
            Textures.RENDER_STATE.set(new CubeRendererState(renderLayer, sideMask, world));
            if (canRenderInLayer(renderLayer)) {
                Material material = pipeTile instanceof TileEntityMaterialPipeBase ? ((TileEntityMaterialPipeBase<?, ?>) pipeTile).getPipeMaterial() : null;
                renderState.lightMatrix.locate(world, pos);

                PipeRenderContext renderContext = new PipeRenderContext(blockPipe, pipeTile, pipeType, material,
                        pipeTile.getVisualConnections(),
                        pipeTile.getBlockedConnections(),
                        pipeType.getThickness(),
                        GTUtility.convertRGBtoOpaqueRGBA_CL(getPipeColor(material, pipeTile.getPaintingColor())),
                        pos, renderState.lightMatrix);
                if (renderLayer == BlockRenderLayer.CUTOUT) {
                    renderPipeBlock(renderState, renderContext);
                    renderFrame(world, pos, pipeTile, renderState, renderContext);
                } else {
                    renderOtherLayers(renderLayer, renderState, renderContext);
                }
            }

            pipeTile.getCoverableImplementation().renderCovers(
                    renderState,
                    new Matrix4().translate(pos.getX(), pos.getY(), pos.getZ()),
                    renderLayer);
            Textures.RENDER_STATE.set(null);
        }
        return true;
    }

    private void renderFrame(IBlockAccess world, BlockPos pos, IPipeTile<?, ?> pipeTile, CCRenderState renderState, PipeRenderContext context) {
        Material frameMaterial = pipeTile.getFrameMaterial();
        if (frameMaterial == null) {
            return;
        }
        BlockFrame block = MetaBlocks.FRAMES.get(frameMaterial);
        if (block == null) {
            return;
        }

        Cuboid6 bounds = Cuboid6.full.copy();

        PipeCoverableImplementation coverable = pipeTile.getCoverableImplementation();

        double thickness = coverable.getCoverPlateThickness();
        for (EnumFacing side : EnumFacing.VALUES) {
            CoverBehavior cover = coverable.getCoverAtSide(side);
            if (cover != null) {
                double sideThickness = cover.getCoverPlateThickness(side, thickness);
                if (sideThickness > 0) {
                    bounds.setSide(side, side.getAxisDirection() == AxisDirection.NEGATIVE ? sideThickness : 1 - sideThickness);
                }
            }
        }

        IBlockState state = block.getBlock(frameMaterial);
        IBakedModel model = Minecraft.getMinecraft().getBlockRendererDispatcher().getModelForState(state);

        try {
            state = state.getActualState(world, pos);
            state = state.getBlock().getExtendedState(state, world, pos);
        } catch (Throwable ignored) {}

        long posRand = MathHelper.getPositionRandom(pos);
        List<BakedQuad> bakedQuads = new ArrayList<>(model.getQuads(state, null, posRand));

        for (EnumFacing side : EnumFacing.VALUES) {
            if (coverable.getCoverAtSide(side) != null) continue; // blocked by cover plates
            if (!context.shouldFrameBeRendered(side)) continue; // skip this side
            bakedQuads.addAll(model.getQuads(state, side, posRand));
        }

        if (bakedQuads.isEmpty()) return;

        List<CCQuad> ccQuads = CCQuad.fromArray(bakedQuads);

        clampQuads(ccQuads, bounds);

        AdvCCRSConsumer lighter = new AdvCCRSConsumer(renderState)
                .setRunPipeline(true);

        renderState.setPipeline(
                new Translation(pos),
                renderState.lightMatrix,
                new ColourMultiplier(GTUtility.convertRGBtoOpaqueRGBA_CL(frameMaterial.getMaterialRGB()))
        );

        for (CCQuad ccQuad : ccQuads) {
            ccQuad.pipe(lighter);
        }
    }

    private static int getPipeColor(@Nullable Material material, int paintingColor) {
        return paintingColor == -1 ?
                material == null ? 0xFFFFFF : material.getMaterialRGB() :
                paintingColor;
    }

    public void renderPipeBlock(CCRenderState renderState, PipeRenderContext renderContext) {
        Cuboid6 cuboid6 = BlockPipe.getSideBox(null, renderContext.getPipeThickness());

        CachedPipeline openFacePipeline = new CachedPipeline(renderContext);
        CachedPipeline sidePipeline = new CachedPipeline(renderContext);
        CachedPipeline blockedPipeline = null;

        buildPipelines(renderContext, openFacePipeline, sidePipeline);

        if ((renderContext.getConnections() & 0b111111) == 0) {
            // base pipe without connections
            for (EnumFacing renderedSide : EnumFacing.VALUES) {
                render(renderState, openFacePipeline, renderedSide, cuboid6);
            }
            return;
        }
        for (EnumFacing renderedSide : EnumFacing.VALUES) {
            if ((renderContext.getConnections() & 0b111111) == (1 << renderedSide.getOpposite().getIndex())) {
                // render open texture if opposite is open and no other
                render(renderState, openFacePipeline, renderedSide, cuboid6);
                continue;
            }
            if (!renderContext.isConnected(renderedSide)) {
                // if connection is blocked, render pipe side
                render(renderState, sidePipeline, renderedSide, cuboid6);
                continue;
            }
            // else render 'branch' section
            Cuboid6 cuboid = BlockPipe.getSideBox(renderedSide, renderContext.getPipeThickness());
            boolean doRenderBlockedOverlay = renderContext.isBlocked(renderedSide);

            for (EnumFacing branchSide : EnumFacing.VALUES) {
                if (branchSide.getAxis() == renderedSide.getAxis()) continue;
                // render side of the branch
                render(renderState, sidePipeline, branchSide, cuboid);
                if (doRenderBlockedOverlay) {
                    // render blocked connections
                    if (blockedPipeline == null) {
                        blockedPipeline = new CachedPipeline(renderContext);
                        buildBlockedOverlayPipeline(renderContext, blockedPipeline);
                    }
                    render(renderState, blockedPipeline, branchSide, cuboid);
                }
            }
            if (renderContext.isConnectedWithSmallerPipe(renderedSide)) {
                // if neighbour pipe is smaller, render closed texture
                render(renderState, sidePipeline, renderedSide, cuboid);
                continue;
            }
            if (renderContext.isCoverAttached(renderedSide)) {
                // if face has a cover offset face by 0.001 to avoid z fighting
                cuboid.setSide(renderedSide, renderedSide.getAxisDirection() == AxisDirection.NEGATIVE ? 0.001 : 0.999);
            } else if (renderContext.isPipeSectionExtruded(renderedSide)) {
                // render extrusion
                cuboid = getExtrusionSideBox(renderedSide, renderContext.getPipeThickness());
                BlockPos pos = renderContext.getPosition() != null ? renderContext.getPosition() : BlockPos.ORIGIN;
                IVertexOperation translation = new Translation(
                        pos.getX() + renderedSide.getXOffset() * PIPE_EXTRUSION_OFFSET,
                        pos.getY() + renderedSide.getYOffset() * PIPE_EXTRUSION_OFFSET,
                        pos.getZ() + renderedSide.getZOffset() * PIPE_EXTRUSION_OFFSET);
                for (EnumFacing extrusionSide : EnumFacing.VALUES) {
                    if (extrusionSide == renderedSide.getOpposite()) continue;
                    if (extrusionSide == renderedSide) {
                        render(renderState, openFacePipeline, extrusionSide, cuboid, translation);
                    } else {
                        render(renderState, sidePipeline, extrusionSide, cuboid, translation);
                        if (doRenderBlockedOverlay) {
                            if (blockedPipeline == null) {
                                blockedPipeline = new CachedPipeline(renderContext);
                                buildBlockedOverlayPipeline(renderContext, blockedPipeline);
                            }
                            render(renderState, blockedPipeline, extrusionSide, cuboid, translation);
                        }
                    }
                }
                continue;
            }
            render(renderState, openFacePipeline, renderedSide, cuboid);
        }
    }

    private void render(CCRenderState renderState, CachedPipeline pipeline, EnumFacing side, Cuboid6 cuboid6) {
        BlockRenderer.BlockFace blockFace = BLOCK_FACE.get();
        blockFace.loadCuboidFace(cuboid6, side.getIndex());
        for (IVertexOperation[] ops : pipeline.getPipelines(side)) {
            renderState.setPipeline(blockFace, 0, blockFace.verts.length, ops);
            renderState.render();
        }
    }

    private void render(CCRenderState renderState, CachedPipeline pipeline, EnumFacing side, Cuboid6 cuboid6, IVertexOperation additionalPipeline) {
        BlockRenderer.BlockFace blockFace = BLOCK_FACE.get();
        blockFace.loadCuboidFace(cuboid6, side.getIndex());
        for (IVertexOperation[] ops : pipeline.getPipelines(side)) {
            renderState.setPipeline(blockFace, 0, blockFace.verts.length, PipelineUtil.concat(ops, additionalPipeline));
            renderState.render();
        }
    }

    @Override
    public void renderBrightness(IBlockState state, float brightness) {}

    /**
     * Override to render in other layers, e.g. emissive stuff
     * {@link #canRenderInLayer} also need to be overridden
     */
    protected void renderOtherLayers(BlockRenderLayer layer, CCRenderState renderState, PipeRenderContext renderContext) {

    }

    /**
     * What layers can be rendered in.
     * See also {@link #renderOtherLayers}
     * @param layer the current layer being rendered too
     * @return true if this should render in {@code layer}
     */
    protected boolean canRenderInLayer(BlockRenderLayer layer) {
        return layer == BlockRenderLayer.CUTOUT;
    }

    @Override
    public void handleRenderBlockDamage(IBlockAccess world, BlockPos pos, IBlockState state, TextureAtlasSprite sprite, BufferBuilder buffer) {
        BlockPipe<?, ?, ?> blockPipe = (BlockPipe<?, ?, ?>) state.getBlock();
        IPipeTile<?, ?> pipeTile = blockPipe.getPipeTileEntity(world, pos);
        if (pipeTile == null) {
            return;
        }
        IPipeType<?> pipeType = pipeTile.getPipeType();
        if (pipeType == null) {
            return;
        }
        CCRenderState renderState = CCRenderState.instance();
        renderState.reset();
        renderState.bind(buffer);
        renderState.setPipeline(new Vector3(new Vec3d(pos)).translation(), new IconTransformation(sprite));
        float thickness = pipeType.getThickness();
        BlockRenderer.renderCuboid(renderState, BlockPipe.getSideBox(null, thickness), 0);
        for (EnumFacing renderSide : EnumFacing.VALUES) {
            if (pipeTile.isConnected(renderSide)) {
                BlockRenderer.renderCuboid(renderState, BlockPipe.getSideBox(renderSide, thickness), 0);
            }
        }
    }

    @Override
    public void registerTextures(TextureMap map) {}

    @Override
    public IModelState getTransforms() {
        return TransformUtils.DEFAULT_BLOCK;
    }

    @Override
    public TextureAtlasSprite getParticleTexture() {
        return TextureUtils.getMissingSprite();
    }

    @Override
    public boolean isBuiltInRenderer() {
        return true;
    }

    @Override
    public boolean isAmbientOcclusion() {
        return true;
    }

    @Override
    public boolean isGui3d() {
        return true;
    }

    public Pair<TextureAtlasSprite, Integer> getParticleTexture(IPipeTile<?, ?> pipeTile) {
        if (pipeTile == null || pipeTile.getPipeType() == null) {
            return Pair.of(TextureUtils.getMissingSprite(), 0xFFFFFF);
        }
        Material material = pipeTile instanceof TileEntityMaterialPipeBase ?
                ((TileEntityMaterialPipeBase<?, ?>) pipeTile).getPipeMaterial() : null;
        return Pair.of(
                getParticleTexture(pipeTile.getPipeType(), material),
                getPipeColor(material, pipeTile.getPaintingColor()));
    }

    protected abstract TextureAtlasSprite getParticleTexture(IPipeType<?> pipeType, @Nullable Material material);

    /**
     * Get cuboid shape for pipe section's 'extended' area. Extended area is a small bit of pipe extruded out on
     * framed pipes.
     *
     * @param side      The specific branch section of the pipe
     * @param thickness Thickness of the pipe
     * @return Cuboid shape for pipe section's extended area
     */
    private static Cuboid6 getExtrusionSideBox(EnumFacing side, float thickness) {
        final float positiveMin = 1 - PIPE_EXTRUSION_OFFSET;
        final float positiveMax = positiveMin + PIPE_EXTRUSION_SIZE;
        final float negativeMin = PIPE_EXTRUSION_OFFSET - PIPE_EXTRUSION_SIZE;
        final float negativeMax = negativeMin + PIPE_EXTRUSION_SIZE;

        float min = (1 - thickness) / 2, max = min + thickness;

        switch (side) {
            case WEST:
                return new Cuboid6(negativeMin, min, min, negativeMax, max, max);
            case EAST:
                return new Cuboid6(positiveMin, min, min, positiveMax, max, max);
            case NORTH:
                return new Cuboid6(min, min, negativeMin, max, max, negativeMax);
            case SOUTH:
                return new Cuboid6(min, min, positiveMin, max, max, positiveMax);
            case UP:
                return new Cuboid6(min, positiveMin, min, max, positiveMax, max);
            case DOWN:
                return new Cuboid6(min, negativeMin, min, max, negativeMax, max);
            default:
                throw new IllegalStateException("Unreachable");
        }
    }

    // helper methods for texture registration

    @Nullable
    public static TextureAtlasSprite texture(TextureMap map, String modid, String name, boolean optional) {
        return optional ? optionalTexture(map, modid, name) : texture(map, modid, name);
    }

    @Nonnull
    public static TextureAtlasSprite texture(TextureMap map, String modid, String name) {
        return map.registerSprite(new ResourceLocation(modid, name));
    }

    @Nullable
    public static TextureAtlasSprite optionalTexture(TextureMap map, String modid, String name) {
        if (ResourceHelper.doResourcepacksHaveTexture(modid, name, true)) {
            return map.registerSprite(new ResourceLocation(modid, name));
        } else {
            return null;
        }
    }

    /**
     * Clamp quads to bounds
     *
     * @param quads  Quads
     * @param bounds Bounds
     */
    // nice javadoc
    // TODO what's CTM? well it's ConnecT Maballs lmao gottem
    private static void clampQuads(List<CCQuad> quads, Cuboid6 bounds) {
        if (quads.isEmpty() || bounds.equals(Cuboid6.full)) return;
        Vertex5[] verticesCache = new Vertex5[]{
                new Vertex5(),
                new Vertex5(),
                new Vertex5(),
                new Vertex5()
        };
        boolean[] clamped = new boolean[4];
        for (CCQuad quad : quads) {
            EnumFacing face = quad.getQuadFace();
            if (face == null) continue;

            for (int i = 0; i < 4; i++) {
                verticesCache[i].set(quad.vertices[i]);
                clamp(verticesCache[i].vec, bounds);
                clamped[i] = !verticesCache[i].vec.equals(quad.vertices[i].vec);
            }

            if (clamped[0] || clamped[1] || clamped[2] || clamped[3]) {
                // calculate barycentric coord for new UV coord
                // or... https://jcgt.org/published/0011/03/04/paper.pdf whatever the hell this is...

                Vector3 v1 = new Vector3(), v2 = new Vector3();

                double[] halfTangents = new double[4];
                double[] weights = new double[4];

                for (int vertexIndex = 0; vertexIndex < 4; vertexIndex++) {
                    if (!clamped[vertexIndex]) continue;

                    Vertex5 vertex = verticesCache[vertexIndex];

                    for (int i = 0; i < 4; i++) {
                        v1.set(quad.vertices[i].vec).subtract(vertex.vec);
                        v2.set(quad.vertices[(i + 1) % 4].vec).subtract(vertex.vec);
                        halfTangents[i] = Math.tan(v1.angle(v2) / 2);
                    }

                    double weightSum = 0;

                    for (int i = 0; i < 4; i++) {
                        int nextIndex = (i + 1) % 4;
                        double wgt = (halfTangents[i] + halfTangents[nextIndex]) /
                                v1.set(quad.vertices[nextIndex].vec).subtract(vertex.vec).mag();

                        weights[nextIndex] = wgt;
                        weightSum += wgt;
                    }

                    vertex.uv.set(0, 0);

                    for (int i = 0; i < 4; i++) {
                        UV uv = quad.vertices[i].uv;
                        vertex.uv.u += uv.u * (weights[i] / weightSum);
                        vertex.uv.v += uv.v * (weights[i] / weightSum);
                    }
                }

                for (int vertexIndex = 0; vertexIndex < 4; vertexIndex++) {
                    if (!clamped[vertexIndex]) continue;

                    Vertex5 vertex = verticesCache[vertexIndex];

                    if (Double.isNaN(vertex.uv.u) || Double.isNaN(vertex.uv.v)) {
                        GTLog.logger.info("@Tictim <<< laugh at this user!!!!!!");
                    } else {
                        quad.vertices[vertexIndex].set(vertex);
                    }
                }
            }
            Arrays.fill(clamped, false);
        }
    }

    private static void clamp(Vector3 vec, Cuboid6 bounds) {
        vec.x = MathHelper.clamp(vec.x, bounds.min.x, bounds.max.x);
        vec.y = MathHelper.clamp(vec.y, bounds.min.y, bounds.max.y);
        vec.z = MathHelper.clamp(vec.z, bounds.min.z, bounds.max.z);
    }
}

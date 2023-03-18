package gregtech.client.renderer.pipe;

import codechicken.lib.lighting.LightMatrix;
import codechicken.lib.render.pipeline.ColourMultiplier;
import codechicken.lib.render.pipeline.IVertexOperation;
import codechicken.lib.vec.Translation;
import codechicken.lib.vec.uv.IconTransformation;
import gregtech.client.renderer.cclop.BottomUVAdjustment;
import gregtech.client.renderer.cclop.DynamicPipeIconTransformation;
import gregtech.client.renderer.cclop.UVList2;
import gregtech.client.utils.PipelineUtil;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;

/**
 * CCL pipeline builder for pipe renderers.
 */
public final class CachedPipeline {

    private final int color;
    private final int connections;
    @Nullable
    private final BlockPos pos;
    @Nullable
    private final LightMatrix lightMatrix;

    /**
     * Pipelines for all faces. Nonnull if this pipeline is side independent.
     */
    private ArrayList<IVertexOperation[]> pipelines = new ArrayList<>();
    /**
     * Pipelines for individual faces. Nonnull if this pipeline is side dependent;
     * i.e. if {@link CachedPipeline#pipelines} is {@code null}.
     */
    private EnumMap<EnumFacing, ArrayList<IVertexOperation[]>> sidePipelines;

    public CachedPipeline(@Nonnull PipeRenderContext context) {
        this.color = context.getColor();
        this.connections = context.getConnections();
        this.pos = context.getPosition();
        this.lightMatrix = context.getLightMatrix();
    }

    @Nonnull
    public ArrayList<IVertexOperation[]> getPipelines(@Nonnull EnumFacing side) {
        return this.pipelines != null ? this.pipelines : this.sidePipelines.get(side);
    }

    public void addSprite(@Nonnull TextureAtlasSprite texture,
                          @Nonnull IVertexOperation... additionalOperations) {
        addSprite(true, texture, additionalOperations);
    }

    public void addSprite(boolean applyDefaultColor,
                          @Nonnull TextureAtlasSprite texture,
                          @Nonnull IVertexOperation... additionalOperations) {
        addOperation(applyDefaultColor, PipelineUtil.concat(
                new UVList2(BottomUVAdjustment.INSTANCE, new IconTransformation(texture)),
                additionalOperations));
    }

    public void addSideSprite(@Nullable TextureAtlasSprite dynamicTexture,
                              @Nonnull TextureAtlasSprite fallbackStaticTexture,
                              @Nonnull IVertexOperation... additionalOperations) {
        addSideSprite(true, dynamicTexture, fallbackStaticTexture, additionalOperations);
    }

    public void addSideSprite(boolean applyDefaultColor,
                              @Nullable TextureAtlasSprite dynamicTexture,
                              @Nonnull TextureAtlasSprite fallbackStaticTexture,
                              @Nonnull IVertexOperation... additionalOperations) {
        if (dynamicTexture == null) {
            addSprite(applyDefaultColor, fallbackStaticTexture, additionalOperations);
        } else {
            for (EnumFacing side : EnumFacing.values()) {
                addSidedOperation(side, applyDefaultColor, PipelineUtil.concat(
                        new UVList2(BottomUVAdjustment.INSTANCE, new DynamicPipeIconTransformation(dynamicTexture, this.connections, side)),
                        additionalOperations));
            }
        }
    }

    public void addOperation(@Nonnull IVertexOperation... vertexOperations) {
        addOperation(true, vertexOperations);
    }

    public void addOperation(boolean applyDefaultColor,
                             @Nonnull IVertexOperation... vertexOperations) {
        ArrayList<IVertexOperation> ops = baseVertexOperation();
        Collections.addAll(ops, vertexOperations);
        if (applyDefaultColor) ops.add(getColorOperation());
        addUniversalPipeline(ops.toArray(new IVertexOperation[0]));
    }

    public void addSidedOperation(@Nonnull EnumFacing side,
                                  @Nonnull IVertexOperation... vertexOperations) {
        addSidedOperation(side, true, vertexOperations);
    }

    public void addSidedOperation(@Nonnull EnumFacing side,
                                  boolean applyDefaultColor,
                                  @Nonnull IVertexOperation... vertexOperations) {
        ArrayList<IVertexOperation> ops = baseVertexOperation();
        Collections.addAll(ops, vertexOperations);
        if (applyDefaultColor) ops.add(getColorOperation());
        addSidedPipeline(side, ops.toArray(new IVertexOperation[0]));
    }

    private void addUniversalPipeline(IVertexOperation[] operations) {
        if (this.pipelines != null) {
            this.pipelines.add(operations);
        } else {
            for (EnumFacing side : EnumFacing.values()) {
                this.sidePipelines.get(side).add(operations);
            }
        }
    }

    private void addSidedPipeline(EnumFacing side, IVertexOperation[] operations) {
        if (this.pipelines != null) {
            // transfer everything registered so far into enum map and empty the non-sided pipeline field
            this.sidePipelines = new EnumMap<>(EnumFacing.class);
            for (EnumFacing side2 : EnumFacing.values()) {
                this.sidePipelines.put(side2, side2 == EnumFacing.NORTH ?
                        this.pipelines : new ArrayList<>(this.pipelines));
            }
            this.pipelines = null;
        }
        this.sidePipelines.get(side).add(operations);
    }

    private ArrayList<IVertexOperation> baseVertexOperation() {
        ArrayList<IVertexOperation> ops = new ArrayList<>();
        if (this.pos != null) ops.add(new Translation(this.pos));
        if (this.lightMatrix != null) ops.add(this.lightMatrix);
        return ops;
    }

    private ColourMultiplier getColorOperation() {
        return new ColourMultiplier(this.color);
    }
}

package gregtech.client.model.component;

import gregtech.client.utils.BloomEffectUtil;
import gregtech.client.utils.RenderUtil;
import it.unimi.dsi.fastutil.ints.IntLinkedOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class ModelStates {

    @Nullable
    public final IBlockState state;

    private final BakedComponent components;
    private final IntSet includedParts = new IntLinkedOpenHashSet();

    private final List<Quad> customQuads = new ArrayList<>();

    public ModelStates(@Nullable IBlockState state, @Nonnull BakedComponent components) {
        this.state = state;
        this.components = components;
    }

    public void includePart(int partIndex) {
        if (partIndex < 0 || partIndex >= this.components.size) {
            if (this.components.size == 0) {
                throw new IndexOutOfBoundsException("No parts registered");
            } else {
                throw new IndexOutOfBoundsException("Invalid part ID " + partIndex + "; expected: 0 ~ " + (this.components.size - 1) + " (inclusive)");
            }
        }
        this.includedParts.add(partIndex);
    }

    public void includeQuad(@Nonnull BakedQuad quad) {
        includeQuad(quad, null);
    }

    public void includeQuad(@Nonnull BakedQuad quad, @Nullable EnumFacing cullFace) {
        includeQuad(quad, cullFace, null);
    }

    public void includeQuad(@Nonnull BakedQuad quad, @Nullable EnumFacing cullFace, boolean emissive) {
        includeQuad(quad, cullFace, emissive ? BloomEffectUtil.BLOOM : null);
    }

    public void includeQuad(@Nonnull BakedQuad quad, @Nullable EnumFacing cullFace, @Nullable BlockRenderLayer specificLayer) {
        includeQuad(new Quad(quad, cullFace, specificLayer));
    }

    public void includeQuad(@Nonnull Quad quad) {
        this.customQuads.add(Objects.requireNonNull(quad, "quad == null"));
    }

    @Nonnull
    public List<BakedQuad> toQuads(@Nullable EnumFacing side, @Nullable BlockRenderLayer currentLayer, boolean bloomActive) {
        List<BakedQuad> quads = new ArrayList<>();
        this.components.addQuads(quads, this.includedParts, side, currentLayer, bloomActive);
        return quads;
    }

    @Override
    public String toString() {
        return "ModelStates{" +
                "state=" + state +
                ", components=" + components +
                ", includedParts=" + includedParts +
                ", customQuads=" + customQuads +
                '}';
    }

    public static final class Quad {

        @Nonnull
        public final BakedQuad quad;
        @Nullable
        public final EnumFacing cullFace;
        @Nullable
        public final BlockRenderLayer specificLayer;

        public Quad(@Nonnull BakedQuad quad, @Nullable EnumFacing cullFace, @Nullable BlockRenderLayer specificLayer) {
            this.quad = Objects.requireNonNull(quad, "quad == null");
            this.cullFace = cullFace;
            this.specificLayer = specificLayer;
        }

        public boolean canRenderOnLayer(@Nullable BlockRenderLayer currentLayer, boolean bloomActive) {
            if (currentLayer == null) return true;

            if (bloomActive) {
                if (this.specificLayer == null) {
                    return currentLayer != BloomEffectUtil.BLOOM;
                } else if (this.specificLayer == BloomEffectUtil.BLOOM) {
                    return currentLayer == BloomEffectUtil.getRealBloomLayer();
                } else {
                    return this.specificLayer == currentLayer;
                }
            }

            if (currentLayer == BloomEffectUtil.BLOOM) {
                return false;
            } else if (this.specificLayer == BloomEffectUtil.BLOOM) {
                return currentLayer == BlockRenderLayer.CUTOUT;
            } else {
                return this.specificLayer == null || this.specificLayer == currentLayer;
            }
        }

        @Override
        public String toString() {
            StringBuilder b = new StringBuilder();
            b.append(RenderUtil.prettyPrintBakedQuad(quad));
            if (cullFace != null) b.append("\nCull Face: ").append(cullFace);
            if (specificLayer != null) b.append("\nRender Layer: ").append(specificLayer);
            return b.toString();
        }
    }
}

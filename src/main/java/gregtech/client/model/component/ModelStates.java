package gregtech.client.model.component;

import gregtech.client.model.ISpecialBakedModel;
import gregtech.client.utils.BloomEffectUtil;
import gregtech.client.utils.RenderUtil;
import gregtech.common.blocks.special.ISpecialState;
import it.unimi.dsi.fastutil.ints.IntLinkedOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class ModelStates {

    @Nullable
    public final ISpecialState state;

    @Nullable
    private final BakedComponent components;
    private final boolean bloomActive;

    @Nullable
    private IntSet includedParts;
    @Nullable
    private List<Quad> customQuads;
    @Nullable
    private List<IBakedModel> subModels;
    @Nullable
    private List<ModelStates> subStates;

    public ModelStates(@Nullable ISpecialState state, @Nullable BakedComponent components, boolean bloomActive) {
        this.state = state;
        this.components = components;
        this.bloomActive = bloomActive;
    }

    public void includePart(int partIndex) {
        if (this.components == null) {
            throw new IllegalStateException("No parts registered");
        }
        if (partIndex < 0 || partIndex >= this.components.size) {
            if (this.components.size == 0) {
                throw new IndexOutOfBoundsException("No parts registered");
            } else {
                throw new IndexOutOfBoundsException("Invalid part ID " + partIndex + "; expected: 0 ~ " + (this.components.size - 1) + " (inclusive)");
            }
        }
        if (this.includedParts == null) this.includedParts = new IntLinkedOpenHashSet();
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
        if (this.customQuads == null) this.customQuads = new ArrayList<>();
        this.customQuads.add(Objects.requireNonNull(quad, "quad == null"));
    }

    public void includeModel(@Nonnull IBakedModel model) {
        if (model instanceof ISpecialBakedModel specialModel) {
            includeSubState(specialModel.collectModels(this.state));
        } else {
            if (this.subModels == null) this.subModels = new ArrayList<>();
            this.subModels.add(model);
        }
    }

    public void includeSubState(@Nonnull ModelStates subState) {
        checkPresence(Objects.requireNonNull(subState, "subState == null"), true);
        if (this.subStates == null) this.subStates = new ArrayList<>();
        this.subStates.add(subState);
    }

    private void checkPresence(@Nonnull ModelStates state, boolean topLevel) {
        if (topLevel && this == state) {
            throw new IllegalStateException("Can't add model state as sub-state of itself");
        }
        if (this.subStates != null) {
            for (ModelStates s : this.subStates) {
                if (s == state) {
                    if (topLevel) throw new IllegalStateException("Duplicated registration of model sub-state");
                    else throw new IllegalStateException("Circular loop of model sub-state");
                }
                s.checkPresence(state, false);
            }
        }
    }

    public void addQuads(@Nonnull List<BakedQuad> quads, @Nullable EnumFacing side, @Nullable BlockRenderLayer currentLayer, long rand) {
        if (this.components != null && this.includedParts != null) {
            this.components.addQuads(quads, this.includedParts, side, currentLayer, this.bloomActive);
        }
        if (this.customQuads != null) {
            for (Quad quad : this.customQuads) {
                if (quad.cullFace == side && quad.canRenderOnLayer(currentLayer, this.bloomActive)) {
                    quads.add(quad.quad);
                }
            }
        }
        if (this.subModels != null) {
            for (IBakedModel subModel : this.subModels) {
                quads.addAll(subModel.getQuads(this.state, side, rand));
            }
        }

        if (this.subStates != null) {
            for (ModelStates subState : this.subStates) {
                subState.addQuads(quads, side, currentLayer, rand);
            }
        }
    }

    @Override
    public String toString() {
        return "ModelStates{" +
                "state=" + state +
                ", components=" + components +
                ", bloomActive=" + bloomActive +
                ", includedParts=" + includedParts +
                ", customQuads=" + customQuads +
                ", subModels=" + subModels +
                ", subStates=" + subStates +
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

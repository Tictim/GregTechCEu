package gregtech.client.model.component;

import it.unimi.dsi.fastutil.ints.IntIterator;
import it.unimi.dsi.fastutil.ints.IntLinkedOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.util.EnumFacing;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;

public final class ModelCollector {

    @Nullable
    public final IBlockState state;

    private final BakedComponent components;
    private final IntSet includedParts = new IntLinkedOpenHashSet();

    public ModelCollector(@Nullable IBlockState state, @Nonnull BakedComponent components) {
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

    @Nonnull
    public List<BakedQuad> toQuads(@Nullable EnumFacing side, boolean includeBloomLayer, boolean includeNonBloomLayer) {
        List<BakedQuad> quads = new ArrayList<>();
        this.components.addQuads(quads, this.includedParts, side, includeBloomLayer, includeNonBloomLayer);
        return quads;
    }

    @Nonnull
    public BitSet toCacheKeys() {
        BitSet bits = new BitSet(this.components.size);
        IntIterator it = this.includedParts.iterator();
        while (it.hasNext()) {
            bits.set(it.nextInt(), true);
        }
        return bits;
    }

    @Override
    public String toString() {
        return "ModelCollector{" +
                "includedParts=" + includedParts +
                '}';
    }
}

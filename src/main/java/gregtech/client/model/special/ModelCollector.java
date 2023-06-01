package gregtech.client.model.special;

import it.unimi.dsi.fastutil.ints.IntIterator;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.util.EnumFacing;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;

public final class ModelCollector {

    @Nullable
    public final IBlockState state;

    private final List<IBakedModel> parts;
    private final IntSet includedParts = new IntOpenHashSet();

    public ModelCollector(@Nullable IBlockState state, @Nonnull List<IBakedModel> parts) {
        this.state = state;
        this.parts = parts;
    }

    public void includePart(int partIndex) {
        if (partIndex < 0 || partIndex >= this.parts.size()) {
            if (this.parts.isEmpty()) {
                throw new IndexOutOfBoundsException("No parts registered");
            } else {
                throw new IndexOutOfBoundsException("Invalid model part index; expected: 0 ~ " + (this.parts.size() - 1) + " (inclusive)");
            }
        }
        this.includedParts.add(partIndex);
    }

    @Nonnull
    public List<BakedQuad> toQuads(@Nullable EnumFacing side, long rand) {
        List<BakedQuad> quads = new ArrayList<>();
        IntIterator it = this.includedParts.iterator();
        while (it.hasNext()) {
            quads.addAll(this.parts.get(it.nextInt()).getQuads(this.state, side, rand));
        }
        return quads;
    }

    @Nonnull
    public BitSet toCacheKeys() {
        BitSet bits = new BitSet(parts.size());
        for (int i = 0; i < parts.size(); i++) {
            if (includedParts.contains(i)) {
                bits.set(i, true);
            }
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

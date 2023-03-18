package gregtech.client.renderer.cclop;

import codechicken.lib.render.CCRenderState;
import codechicken.lib.vec.ITransformation;
import codechicken.lib.vec.uv.UV;
import codechicken.lib.vec.uv.UVTransformation;
import codechicken.lib.vec.uv.UVTransformationList;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;

import java.util.ArrayList;

/**
 * {@link UVTransformationList} but calls {@link UVTransformation#operate(CCRenderState)} instead of {@link UVTransformation#apply(UV)}
 */
public class UVList2 extends UVTransformationList {

    private final ArrayList<UVTransformation> transformations = new ArrayList<>();

    public UVList2(UVTransformation... transforms) {
        for (UVTransformation t : transforms) {
            if (t instanceof UVList2) {
                transformations.addAll(((UVList2) t).transformations);
            } else {
                transformations.add(t);
            }
        }

        compact();
    }

    @Override
    public void operate(CCRenderState state) {
        TextureAtlasSprite sprite = state.sprite;
        for (UVTransformation transformation : transformations) {
            transformation.operate(state);
            if (state.sprite != null) sprite = state.sprite;
            else state.sprite = sprite;
        }
    }

    @Override
    public void apply(UV uv) {
        for (UVTransformation transformation : transformations) {
            transformation.apply(uv);
        }
    }

    /**
     * @deprecated Fuck you CCL
     */
    @Override
    @Deprecated
    public UVTransformationList with(UVTransformation t) {
        return super.with(t);
    }

    private void compact() {
        ArrayList<UVTransformation> newList = new ArrayList<>(transformations.size());
        UVTransformation prev = null;
        for (UVTransformation t : transformations) {
            if (t.isRedundant()) {
                continue;
            }

            if (prev != null) {
                UVTransformation m = prev.merge(t);
                if (m == null) {
                    newList.add(prev);
                } else t = m.isRedundant() ? null : m;
            }
            prev = t;
        }
        if (prev != null) {
            newList.add(prev);
        }

        if (newList.size() < transformations.size()) {
            this.transformations.clear();
            this.transformations.addAll(newList);
        }
    }

    @Override
    public boolean isRedundant() {
        return transformations.size() == 0;
    }

    @Override
    public UVTransformation inverse() {
        return new UVList2(transformations.stream()
                .map(ITransformation::inverse)
                .toArray(UVTransformation[]::new));
    }

    @Override
    public String toString() {
        StringBuilder s = new StringBuilder();
        for (UVTransformation t : transformations) {
            s.append("\n").append(t.toString());
        }
        return s.toString().trim();
    }
}

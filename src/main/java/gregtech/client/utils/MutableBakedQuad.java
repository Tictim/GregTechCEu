package gregtech.client.utils;

import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.client.model.pipeline.LightUtil;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
public class MutableBakedQuad extends BakedQuad {

    public MutableBakedQuad(int[] vertexDataIn,
                            int tintIndexIn,
                            EnumFacing faceIn,
                            TextureAtlasSprite spriteIn,
                            boolean applyDiffuseLighting,
                            VertexFormat format) {
        super(vertexDataIn.clone(), tintIndexIn, faceIn, spriteIn, applyDiffuseLighting, format);
    }

    public MutableBakedQuad(BakedQuad quad) {
        super(quad.getVertexData().clone(),
                quad.getTintIndex(),
                quad.getFace(),
                quad.getSprite(),
                quad.shouldApplyDiffuseLighting(),
                quad.getFormat());
    }

    public void putData(int vertex, int elementId, float... data) {
        LightUtil.pack(data, this.vertexData, this.format, vertex, elementId);
    }
}

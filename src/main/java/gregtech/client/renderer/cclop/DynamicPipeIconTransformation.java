package gregtech.client.renderer.cclop;

import codechicken.lib.vec.uv.UV;
import codechicken.lib.vec.uv.UVTransformation;
import gregtech.client.renderer.pipe.FaceConnection;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.EnumFacing;

/**
 * IconTransformation for pipe side textures. Changes UV based on connection states.
 */
public class DynamicPipeIconTransformation extends UVTransformation {

    private final TextureAtlasSprite texture;
    private final int uindex, vindex;

    public DynamicPipeIconTransformation(TextureAtlasSprite texture, byte faceConnection) {
        this.texture = texture;
        if (FaceConnection.connectedToLeft(faceConnection)) {
            this.uindex = FaceConnection.connectedToRight(faceConnection) ? 2 : 3;
        } else {
            this.uindex = FaceConnection.connectedToRight(faceConnection) ? 1 : 0;
        }
        if (FaceConnection.connectedToDown(faceConnection)) {
            this.vindex = FaceConnection.connectedToUp(faceConnection) ? 1 : 0;
        } else {
            this.vindex = FaceConnection.connectedToUp(faceConnection) ? 2 : 3;
        }
    }

    public DynamicPipeIconTransformation(TextureAtlasSprite texture, int connections, EnumFacing side) {
        this(texture, FaceConnection.forFace(connections, side));
    }

    @Override
    public void apply(UV uv) {
        uv.u = this.texture.getInterpolatedU((uv.u + this.uindex) * 4.0);
        uv.v = this.texture.getInterpolatedV((uv.v + this.vindex) * 4.0);
    }

    @Override
    public UVTransformation inverse() {
        return new UVTransformation() {

            @Override
            public void apply(UV uv) {
                // is this even correct??? do i really need to provide this??????????????
                TextureAtlasSprite texture = DynamicPipeIconTransformation.this.texture;
                uv.u = texture.getUnInterpolatedU((float) uv.u) / 4.0 - DynamicPipeIconTransformation.this.uindex;
                uv.v = texture.getUnInterpolatedV((float) uv.v) / 4.0 - DynamicPipeIconTransformation.this.vindex;
            }

            @Override
            public UVTransformation inverse() {
                return DynamicPipeIconTransformation.this;
            }
        };
    }

    private static boolean isConnected(int connections, EnumFacing side) {
        return (connections & 1 << side.getIndex()) != 0;
    }
}

package gregtech.client.model.pipe;

import gregtech.client.model.component.ComponentModel;
import gregtech.client.model.component.ComponentTexture;
import gregtech.client.model.component.IComponentLogic;
import gregtech.client.model.component.ModelTextureMapping;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

import javax.annotation.Nonnull;

public class LaserPipeModelLogicProvider extends PipeModelLogicProvider {

    protected static final String TEXTURE_OVERLAY_ATLAS = "#overlay_atlas";
    protected static final String TEXTURE_OVERLAY_ATLAS_JOINTED = "#overlay_atlas_jointed";
    protected static final String TEXTURE_SIDE_OVERLAY = "#side_overlay";
    protected static final String TEXTURE_EXTRUSION_OVERLAY = "#extrusion";

    protected static final String TEXTURE_EMISSIVE_ATLAS = "#emissive_atlas";
    protected static final String TEXTURE_EMISSIVE_ATLAS_JOINTED = "#emissive_atlas_jointed";
    protected static final String TEXTURE_SIDE_EMISSIVE = "#side_emissive";

    public static final PipeModelTexture TEXTURES = new PipeModelTexture(
            new PipeSideAtlasTexture(
                    new PipeSideAtlasTexture(
                            DEFAULT_TEXTURES.atlas,
                            TEXTURE_OVERLAY_ATLAS,
                            TINT_OVERLAY),
                    TEXTURE_EMISSIVE_ATLAS,
                    -1
            ),
            new PipeSideAtlasTexture(
                    new PipeSideAtlasTexture(
                            DEFAULT_TEXTURES.jointedAtlas,
                            TEXTURE_OVERLAY_ATLAS_JOINTED,
                            TINT_OVERLAY),
                    TEXTURE_EMISSIVE_ATLAS_JOINTED,
                    -1
            ),
            new ComponentTexture(
                    new ComponentTexture(
                            DEFAULT_TEXTURES.side,
                            TEXTURE_SIDE_OVERLAY,
                            TINT_OVERLAY),
                    TEXTURE_SIDE_EMISSIVE,
                    -1),
            DEFAULT_TEXTURES.open,
            new ComponentTexture(
                    DEFAULT_TEXTURES.extrusion,
                    TEXTURE_EXTRUSION_OVERLAY,
                    TINT_OVERLAY)
    );

    public LaserPipeModelLogicProvider(float thickness) {
        super(thickness);
    }

    @Nonnull
    @Override
    public ModelTextureMapping getDefaultTextureMappings() {
        Object2ObjectOpenHashMap<String, String> map = new Object2ObjectOpenHashMap<>();
        map.put(TEXTURE_ATLAS_JOINTED, TEXTURE_ATLAS);
        map.put(TEXTURE_EXTRUSION, TEXTURE_SIDE);
        map.put(TEXTURE_OVERLAY_ATLAS_JOINTED, TEXTURE_OVERLAY_ATLAS);
        map.put(TEXTURE_EMISSIVE_ATLAS_JOINTED, TEXTURE_EMISSIVE_ATLAS);
        return new ModelTextureMapping(map);
    }

    @Nonnull
    @Override
    public IComponentLogic buildLogic(@Nonnull ComponentModel.Register componentRegister, @Nonnull ModelTextureMapping textureMapping) {
        return new LaserPipeModelLogic(
                defaultBaseModels(componentRegister, textureMapping),
                defaultEndModels(componentRegister, textureMapping, false),
                defaultEndModels(componentRegister, textureMapping, true),
                defaultExtrusionModels(componentRegister, textureMapping, false),
                defaultExtrusionModels(componentRegister, textureMapping, true));
    }

    @Override
    protected PipeModelTexture getModelTextures() {
        return TEXTURES;
    }
}

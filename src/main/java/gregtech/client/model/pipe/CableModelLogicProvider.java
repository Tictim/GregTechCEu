package gregtech.client.model.pipe;

import gregtech.client.model.component.*;
import net.minecraft.util.EnumFacing;

import javax.annotation.Nonnull;

public class CableModelLogicProvider extends PipeModelLogicProvider {

    public static final String TEXTURE_WIRE = "#wire";
    public static final String TEXTURE_INSULATION_ATLAS = "#insulation_atlas";
    public static final String TEXTURE_INSULATION_ATLAS_JOINTED = "#insulation_atlas_jointed";
    public static final String TEXTURE_INSULATION_SIDE = "#insulation_side";
    public static final String TEXTURE_IN_OVERLAY = "#in_overlay";
    public static final String TEXTURE_INSULATION_EXTRUSION = "#insulation_extrusion";

    public static final PipeModelTexture INSULATED_TEXTURES = new PipeModelTexture(
            new PipeSideAtlasTexture(DEFAULT_TEXTURES.atlas, TEXTURE_INSULATION_ATLAS, TINT_OVERLAY),
            new PipeSideAtlasTexture(DEFAULT_TEXTURES.atlas, TEXTURE_INSULATION_ATLAS_JOINTED, TINT_OVERLAY),
            new ComponentTexture(TEXTURE_INSULATION_SIDE, TINT_OVERLAY),
            new ComponentTexture(DEFAULT_TEXTURES.in, TEXTURE_IN_OVERLAY, TINT_OVERLAY),
            new ComponentTexture(TEXTURE_INSULATION_EXTRUSION, TINT_OVERLAY)
    );

    private final boolean insulated;

    public CableModelLogicProvider(float thickness, boolean insulated) {
        super(thickness);
        this.insulated = insulated;
    }

    @Nonnull
    @Override
    public ModelTextureMapping getDefaultTextureMappings() {
        return ModelTextureMapping.builder(super.getDefaultTextureMappings())
                .add(TEXTURE_INSULATION_ATLAS_JOINTED, TEXTURE_INSULATION_ATLAS)
                .add(TEXTURE_INSULATION_EXTRUSION, TEXTURE_INSULATION_SIDE)
                .add(TEXTURE_SIDE, TEXTURE_WIRE)
                .add(TEXTURE_IN, TEXTURE_WIRE)
                .build();
    }

    @Nonnull
    @Override
    public IComponentLogic buildLogic(@Nonnull ComponentModel.Register componentRegister,
                                      @Nonnull ModelTextureMapping textureMapping) {
        return new CableModelLogic(
                defaultBaseModels(componentRegister, textureMapping),
                defaultEndModels(componentRegister, textureMapping, true),
                defaultEndModels(componentRegister, textureMapping, false),
                defaultExtrusionModels(componentRegister, textureMapping, true),
                defaultExtrusionModels(componentRegister, textureMapping, false));
    }

    @Override
    protected PipeModelTexture getModelTextures() {
        return this.insulated ? INSULATED_TEXTURES : DEFAULT_TEXTURES;
    }

    @Override
    protected int connectionlessModel(@Nonnull ComponentModel.Register componentRegister,
                                      @Nonnull ModelTextureMapping textureMapping,
                                      @Nonnull PipeModelTexture textures) {
        Component c = new Component(
                modelStart, modelStart, modelStart,
                modelEnd, modelEnd, modelEnd);
        PipeSideAtlasTexture sideAtlas = textures.sideAtlas(true);
        if (sideAtlas != null && textureMapping.has(sideAtlas.getTextureName())) {
            sideAtlas.setAtlasTexture(c, 0, EnumFacing.VALUES);
        } else if (textures.side != null) {
            c.addAllFaces(textures.side);
        }
        return componentRegister.add(c);
    }
}

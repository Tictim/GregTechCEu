package gregtech.client.renderer.pipe;

import codechicken.lib.render.pipeline.ColourMultiplier;
import codechicken.lib.texture.TextureUtils;
import gregtech.api.GTValues;
import gregtech.api.pipenet.block.IPipeType;
import gregtech.api.pipenet.block.material.TileEntityMaterialPipeBase;
import gregtech.api.pipenet.tile.IPipeTile;
import gregtech.api.unification.material.Material;
import gregtech.api.util.GTUtility;
import gregtech.common.pipelike.cable.Insulation;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.util.ResourceLocation;
import org.apache.commons.lang3.tuple.Pair;

import javax.annotation.Nullable;
import java.util.EnumMap;

public class CableRenderer extends PipeRenderer {

    public static final CableRenderer INSTANCE = new CableRenderer();

    private TextureAtlasSprite wireSide;

    // 0~5 for insulated open faces, 6 for insulated cable sides because fuck me
    private final TextureAtlasSprite[] insulationTextures = new TextureAtlasSprite[6];

    private final EnumMap<Insulation, TextureAtlasSprite> dynamicWires = new EnumMap<>(Insulation.class);

    private CableRenderer() {
        super("gt_cable", new ResourceLocation(GTValues.MODID, "cable"));
    }


    @Override
    protected void registerPipeTextures(TextureMap map) {
        this.wireSide = texture(map, GTValues.MODID, "blocks/cable/wire");
        for (int i = 0; i < insulationTextures.length; i++) {
            this.insulationTextures[i] = texture(map, GTValues.MODID, "blocks/cable/insulation_" + i);
        }
        for (Insulation insulation : Insulation.values()) {
            String textureName = insulation.insulationLevel >= 0 ? "insulation_" + insulation.insulationLevel : insulation.name;
            this.dynamicWires.put(insulation, optionalTexture(map, GTValues.MODID, "blocks/cable/dynamic/" + textureName));
        }
    }

    @Override
    protected void buildPipelines(PipeRenderContext context, CachedPipeline openFace, CachedPipeline side) {
        if (context.getMaterial() == null || !(context.getPipeType() instanceof Insulation)) {
            return;
        }

        Insulation insulation = (Insulation) context.getPipeType();
        ColourMultiplier wireColor = new ColourMultiplier(GTUtility.convertRGBtoOpaqueRGBA_CL(context.getMaterial().getMaterialRGB()));
        ColourMultiplier insulationColor = new ColourMultiplier(GTUtility.convertRGBtoOpaqueRGBA_CL(0x404040));
        if (context.getPipeTile() != null) {
            if (context.getPipeTile().getPaintingColor() != context.getPipeTile().getDefaultPaintingColor()) {
                wireColor.colour = GTUtility.convertRGBtoOpaqueRGBA_CL(context.getPipeTile().getPaintingColor());
            }
            insulationColor.colour = GTUtility.convertRGBtoOpaqueRGBA_CL(context.getPipeTile().getPaintingColor());
        }

        if (insulation.insulationLevel != -1) {
            if ((context.getConnections() & 0b111111) == 0) {
                // render only insulation when cable has no connections
                openFace.addSprite(false, this.insulationTextures[5], insulationColor);
            } else {
                openFace.addSprite(false, wireSide, wireColor);
                openFace.addSprite(false, insulationTextures[insulation.insulationLevel], insulationColor);
                side.addSideSprite(false, dynamicWires.get(insulation), insulationTextures[5], insulationColor);
            }
        } else {
            openFace.addSprite(false, wireSide, wireColor);
            side.addSideSprite(false, dynamicWires.get(insulation), wireSide, wireColor);
        }
    }

    @Override
    public TextureAtlasSprite getParticleTexture(IPipeType<?> pipeType, @Nullable Material material) {
        return null;
    }

    @Override
    public Pair<TextureAtlasSprite, Integer> getParticleTexture(IPipeTile<?, ?> pipeTile) {
        if (pipeTile != null) {
            IPipeType<?> pipeType = pipeTile.getPipeType();
            if (pipeType instanceof Insulation) {
                if (((Insulation) pipeType).insulationLevel == -1) {
                    Material material = pipeTile instanceof TileEntityMaterialPipeBase ?
                            ((TileEntityMaterialPipeBase<?, ?>) pipeTile).getPipeMaterial() : null;
                    return Pair.of(wireSide, material == null ? 0xFFFFFF : material.getMaterialRGB());
                } else {
                    return Pair.of(insulationTextures[5], pipeTile.getPaintingColor());
                }
            }
        }
        return Pair.of(TextureUtils.getMissingSprite(), 0xFFFFFF);
    }
}

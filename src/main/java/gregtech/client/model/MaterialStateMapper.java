package gregtech.client.model;

import gregtech.api.unification.material.Material;
import gregtech.client.model.modelfactories.MaterialBlockModelLoader;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.client.renderer.block.statemap.IStateMapper;

import javax.annotation.Nonnull;
import java.util.Map;
import java.util.function.Function;

public class MaterialStateMapper implements IStateMapper {

    private final Map<Material, MaterialBlockModelLoader.Entry> entryMap;
    private final Function<IBlockState, Material> materialFunction;

    public MaterialStateMapper(@Nonnull Map<Material, MaterialBlockModelLoader.Entry> entryMap,
                               @Nonnull Function<IBlockState, Material> materialFunction) {
        this.entryMap = entryMap;
        this.materialFunction = materialFunction;
    }

    @Override
    public Map<IBlockState, ModelResourceLocation> putStateModelLocations(Block b) {
        Map<IBlockState, ModelResourceLocation> map = new Object2ObjectOpenHashMap<>();
        for (IBlockState state : b.getBlockState().getValidStates()) {
            Material m = materialFunction.apply(state);
            MaterialBlockModelLoader.Entry entry = entryMap.get(m);
            if (entry == null) map.put(state, new ModelResourceLocation("missing"));
            else map.put(state, entry.getBlockModelLocation());
        }
        return map;
    }
}

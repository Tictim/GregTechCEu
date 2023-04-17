package gregtech.api.util;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.IProperty;
import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;

import javax.annotation.Nonnull;
import java.util.Comparator;
import java.util.Map;
import java.util.stream.Stream;

public class BlockUtility {

    private static final BlockWrapper WRAPPER = new BlockWrapper();

    private static class BlockWrapper extends Block {

        public BlockWrapper() {
            super(Material.AIR);
        }

        @Nonnull
        @Override
        public NonNullList<ItemStack> captureDrops(boolean start) {
            return super.captureDrops(start);
        }
    }

    public static void startCaptureDrops() {
        WRAPPER.captureDrops(true);
    }

    public static NonNullList<ItemStack> stopCaptureDrops() {
        return WRAPPER.captureDrops(false);
    }

    public static String statePropertiesToString(Map<IProperty<?>, Comparable<?>> properties) {
        return statePropertiesToString(properties.entrySet().stream());
    }

    public static String statePropertiesToString(Stream<Map.Entry<IProperty<?>, Comparable<?>>> properties) {
        StringBuilder stb = new StringBuilder();

        properties.sorted(Comparator.comparing(c -> c.getKey().getName()))
                .forEachOrdered(entry -> {
                    if (stb.length() != 0) {
                        stb.append(",");
                    }

                    IProperty<?> property = entry.getKey();
                    stb.append(property.getName());
                    stb.append("=");
                    stb.append(getPropertyName(property, entry.getValue()));
                });

        return stb.length() == 0 ? "normal" : stb.toString();
    }

    @SuppressWarnings("unchecked")
    private static <T extends Comparable<T>> String getPropertyName(IProperty<T> property, Comparable<?> value) {
        return property.getName((T) value);
    }
}

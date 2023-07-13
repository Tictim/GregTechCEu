package gregtech.api.recipes.machines;

import com.google.common.util.concurrent.AtomicDouble;
import gregtech.api.capability.impl.FluidTankList;
import gregtech.api.gui.GuiTextures;
import gregtech.api.gui.ModularUI;
import gregtech.api.gui.widgets.ProgressWidget;
import gregtech.api.gui.widgets.RecipeProgressWidget;
import gregtech.api.recipes.RecipeBuilder;
import gregtech.api.recipes.RecipeMap;
import net.minecraftforge.items.IItemHandlerModifiable;
import org.apache.commons.lang3.tuple.Pair;

import java.util.function.DoubleSupplier;

public class RecipeMapCrackerUnit<R extends RecipeBuilder<R>> extends RecipeMap<R> {

    public RecipeMapCrackerUnit(String unlocalizedName, int maxInputs, boolean modifyItemInputs, int maxOutputs, boolean modifyItemOutputs,
                                 int maxFluidInputs, boolean modifyFluidInputs, int maxFluidOutputs, boolean modifyFluidOutputs, R defaultRecipe, boolean isHidden) {
        super(unlocalizedName, maxInputs, modifyItemInputs, maxOutputs, modifyItemOutputs, maxFluidInputs, modifyFluidInputs, maxFluidOutputs, modifyFluidOutputs, defaultRecipe, isHidden);
    }

    @Override
    public ModularUI.Builder createJeiUITemplate(IItemHandlerModifiable importItems, IItemHandlerModifiable exportItems, FluidTankList importFluids, FluidTankList exportFluids, int yOffset) {
        ModularUI.Builder builder = ModularUI.defaultBuilder(yOffset);
        if (getMaxInputs() == 1) {
            addSlot(builder, 52, 24 + yOffset, 0, importItems, importFluids, false, false);
        } else {
            int[] grid = determineSlotsGrid(getMaxInputs());
            for (int y = 0; y < grid[1]; y++) {
                for (int x = 0; x < grid[0]; x++) {
                    addSlot(builder, 34 + (x * 18) - (Math.max(0, grid[0] - 2) * 18), 24 + (y * 18) - (Math.max(0, grid[1] - 1) * 18) + yOffset,
                            y * grid[0] + x, importItems, importFluids, false, false);
                }
            }
        }

        addInventorySlotGroup(builder, exportItems, exportFluids, true, yOffset);
        addSlot(builder, 52, 24 + yOffset + 19 + 18, 0, importItems, importFluids, true, false);
        addSlot(builder, 34, 24 + yOffset + 19 + 18, 1, importItems, importFluids, true, false);

        Pair<DoubleSupplier, DoubleSupplier> suppliers = createPairedSupplier(200, 41, 0.5);
        builder.widget(new RecipeProgressWidget(suppliers.getLeft(), 42, 24 + yOffset + 18, 21, 19, GuiTextures.PROGRESS_BAR_CRACKING_INPUT, ProgressWidget.MoveType.VERTICAL, this));
        builder.widget(new RecipeProgressWidget(suppliers.getRight(), 78, 23 + yOffset, 20, 20, progressBarTexture, moveType, this));
        return builder;
    }

    public static Pair<DoubleSupplier, DoubleSupplier> createPairedSupplier(int ticksPerCycle, int width, double splitPoint) {
        AtomicDouble tracker = new AtomicDouble(0.0);
        DoubleSupplier supplier1 = new ProgressWidget.TimedProgressSupplier(ticksPerCycle, width, false) {
            @Override
            public double getAsDouble() {
                double val = super.getAsDouble();
                tracker.set(val);
                return val >= splitPoint ? 1.0 : (1.0 / splitPoint) * val;
            }
        };
        DoubleSupplier supplier2 = () -> tracker.get() >= splitPoint ? (1.0 / (1 - splitPoint)) * (tracker.get() - splitPoint) : 0;
        return Pair.of(supplier1, supplier2);
    }
}

package damege.mekanismcard.data;

import damege.mekanismcard.MekanismCard;
import mekanism.common.registries.MekanismItems;
import mekanism.common.tags.MekanismTags;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.recipes.ShapedRecipeBuilder;

import java.util.function.Consumer;

public class ModRecipeProvider extends RecipeProvider {
    public ModRecipeProvider(PackOutput output) {
        super(output);
    }

    @Override
    protected void buildRecipes(Consumer<FinishedRecipe> output) {
        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, MekanismCard.MASS_UPGRADE_CONFIGURATOR.get())
                .pattern("121")
                .pattern("232")
                .pattern("121")
                .define('1', MekanismTags.Items.ALLOYS_ULTIMATE)
                .define('2', MekanismTags.Items.CIRCUITS_ULTIMATE)
                .define('3', MekanismItems.CONFIGURATION_CARD)
                .unlockedBy("has_alloy", has(MekanismTags.Items.ALLOYS_ULTIMATE))
                .unlockedBy("has_circuit", has(MekanismTags.Items.CIRCUITS_ULTIMATE))
                .unlockedBy("has_card", has(MekanismItems.CONFIGURATION_CARD))
                .save(output);

        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, MekanismCard.MEMORY_CARD.get())
                .pattern("121")
                .pattern("343")
                .pattern("252")
                .define('1', MekanismItems.HDPE_SHEET)
                .define('2', MekanismItems.POLONIUM_PELLET)
                .define('3', MekanismTags.Items.CIRCUITS_ULTIMATE)
                .define('4', MekanismItems.CONFIGURATION_CARD)
                .define('5', MekanismItems.BASE_QIO_DRIVE)
                .unlockedBy("has_hdpe", has(MekanismItems.HDPE_SHEET))
                .unlockedBy("has_circuit", has(MekanismTags.Items.CIRCUITS_ULTIMATE))
                .unlockedBy("has_card", has(MekanismItems.CONFIGURATION_CARD))
                .save(output);
    }
}

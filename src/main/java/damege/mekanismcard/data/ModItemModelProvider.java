package damege.mekanismcard.data;

import damege.mekanismcard.MekanismCard;
import net.minecraft.data.PackOutput;
import net.minecraftforge.client.model.generators.ItemModelProvider;
import net.minecraftforge.common.data.ExistingFileHelper;

public class ModItemModelProvider extends ItemModelProvider {
    public ModItemModelProvider(PackOutput output, ExistingFileHelper existingFileHelper) {
        super(output, MekanismCard.MOD_ID, existingFileHelper);
    }

    @Override
    protected void registerModels() {
        basicItem(MekanismCard.MEMORY_CARD.get());
        basicItem(MekanismCard.MASS_UPGRADE_CONFIGURATOR.get());
    }
}

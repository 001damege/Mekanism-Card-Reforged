package damege.mekanismcard;

import damege.mekanismcard.data.ModItemModelProvider;
import damege.mekanismcard.data.ModLanguageProvider;
import damege.mekanismcard.data.ModRecipeProvider;
import damege.mekanismcard.item.MassUpgradeConfigurator;
import damege.mekanismcard.item.MemoryCard;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.data.ExistingFileHelper;
import net.minecraftforge.data.event.GatherDataEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

@Mod(MekanismCard.MOD_ID)
public class MekanismCard {
    public static final String MOD_ID = "mekanism_card";

    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MOD_ID);
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MOD_ID);

    public static final RegistryObject<Item> MEMORY_CARD = ITEMS.register("memory_card", MemoryCard::new);
    public static final RegistryObject<Item> MASS_UPGRADE_CONFIGURATOR = ITEMS.register("mass_upgrade_configurator", MassUpgradeConfigurator::new);

    public static final RegistryObject<CreativeModeTab> MAIN = TABS.register("main", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.mekanism_card"))
            .icon(() -> new ItemStack(MASS_UPGRADE_CONFIGURATOR.get()))
            .displayItems((params, out) -> {
                out.accept(MASS_UPGRADE_CONFIGURATOR.get());
                out.accept(MEMORY_CARD.get());
            }).build());

    public MekanismCard(FMLJavaModLoadingContext ctx) {
        IEventBus eventBus = ctx.getModEventBus();

        ITEMS.register(eventBus);
        TABS.register(eventBus);
        eventBus.addListener(this::gatherData);
    }

    private void gatherData(GatherDataEvent event) {
        DataGenerator gen = event.getGenerator();
        PackOutput output = gen.getPackOutput();
        ExistingFileHelper efh = event.getExistingFileHelper();

        if (event.includeClient()) {
            gen.addProvider(true, new ModLanguageProvider(output));
            gen.addProvider(true, new ModItemModelProvider(output, efh));
        }

        if (event.includeServer()) {
            gen.addProvider(true, new ModRecipeProvider(output));
        }
    }
}

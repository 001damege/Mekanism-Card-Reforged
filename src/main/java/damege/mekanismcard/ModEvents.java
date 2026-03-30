package damege.mekanismcard;

import damege.mekanismcard.item.MassUpgradeConfigurator;
import damege.mekanismcard.item.MemoryCard;
import mekanism.api.IConfigCardAccess;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = MekanismCard.MOD_ID)
public class ModEvents {

    @SubscribeEvent
    public static void onRightClicked(PlayerInteractEvent.RightClickBlock event) {
        Player player = event.getEntity();
        ItemStack stack = event.getItemStack();
        if (stack.getItem() instanceof MemoryCard) {
            if (event.getLevel().isClientSide) {
                return;
            }

            BlockPos pos = event.getPos();
            boolean isTargetingMachine = event.getLevel().getBlockEntity(pos) instanceof IConfigCardAccess;
            if (player.isShiftKeyDown()) {
                MemoryCard.handleClearStatic(player, stack);
            } else if (isTargetingMachine) {
                if (MemoryCard.hasTag(stack)) {
                    MemoryCard.handlePasteStatic(event.getLevel(), pos, player, stack);
                } else {
                    MemoryCard.handleCopyStatic(event.getLevel(), pos, player, stack);
                }
            }

            event.setCanceled(true);
            return;
        }

        if (!(stack.getItem() instanceof MassUpgradeConfigurator configurator)) {
            return;
        }
        if (event.getLevel().isClientSide) {
            return;
        }

        displayModeInfo(player, configurator, stack);
        boolean selectionMode = configurator.isSelectionModeActive(stack);
        if (selectionMode) {
            configurator.checkAndClearSelectionIfTooFar(event.getLevel(), player, stack);
            if (player.isShiftKeyDown()) {
                configurator.handleSelectionModeSetPoint(event.getLevel(), event.getPos(), player, stack);
                event.setCanceled(true);
            } else {
                configurator.handleSelectionModeExecute(event.getLevel(), event.getPos(), player, stack);
                event.setCanceled(true);
            }
        } else {
            if (player.isShiftKeyDown()) {
                configurator.handleRadiusMode(event.getLevel(), event.getPos(), player);
                event.setCanceled(true);
            } else {
                event.setCanceled(true);
            }
        }
    }

    @SubscribeEvent
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        Player player = event.getEntity();
        ItemStack stack = event.getItemStack();
        if (stack.getItem() instanceof MemoryCard) {
            if (event.getLevel().isClientSide()) {
                return;
            }

            if (player.isShiftKeyDown()) {
                MemoryCard.handleClearStatic(player, stack);
                event.setCanceled(true);
            }
        }
    }

    private static void displayModeInfo(Player player, MassUpgradeConfigurator configurator, ItemStack stack) {
        boolean selectionMode = configurator.isSelectionModeActive(stack);
        String modeKey = selectionMode ? "tooltip.mekanism_card.mode.selection" : "tooltip.mekanism_card.mode.radius";
        player.displayClientMessage(Component.translatable("tooltip.mekanism_card.current_mode",
                Component.translatable(modeKey), configurator.getCurrentMode().getDisplayName())
                .withStyle(selectionMode ? ChatFormatting.AQUA : ChatFormatting.GOLD), true);
    }
}

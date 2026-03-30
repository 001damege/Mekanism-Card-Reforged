package damege.mekanismcard.data;

import damege.mekanismcard.MekanismCard;
import net.minecraft.data.PackOutput;
import net.minecraftforge.common.data.LanguageProvider;

public class ModLanguageProvider extends LanguageProvider {
    public ModLanguageProvider(PackOutput output) {
        super(output, MekanismCard.MOD_ID, "en_us");
    }

    @Override
    protected void addTranslations() {
        add(MekanismCard.MEMORY_CARD.get(), "Memory Card");
        add(MekanismCard.MASS_UPGRADE_CONFIGURATOR.get(), "Mass Upgrade Configurator");

        add("itemGroup.mekanism_card", "Mekanism Card");

        add("mekanism_card.mode.install", "Install Mode");
        add("mekanism_card.mode.remove", "Remove Mode");

        add("mekanism_card.memory_card.mode.copy", "Copy Mode");
        add("mekanism_card.memory_card.mode.paste", "Paste Mode");
        add("message.mekanism_card.memory_card.mode_switched", "Switched to %s");
        add("message.mekanism_card.memory_card.copied", "Copied configuration from %s machines");
        add("message.mekanism_card.memory_card.pasted", "Pasted successfully!");
        add("message.mekanism_card.memory_card.pasted_creative", "Pasted successfully (creative)!");
        add("message.mekanism_card.memory_card.not_enough_upgrades", "Not enough upgrade cards!");
        add("message.mekanism_card.memory_card.type_mismatch", "Machine type mismatch!");
        add("message.mekanism_card.memory_card.nothing_to_copy", "No tag to copy from this machine");
        add("message.mekanism_card.memory_card.no_tag", "No tag stored on memory card");
        add("message.mekanism_card.memory_card.not_mekanism", "Target is not a Mekanism machine");
        add("message.mekanism_card.memory_card.paste_failed", "Paste failed");
        add("tooltip.mekanism_card.memory_card.air_click_switch_mode", "Air right-click: Switch copy/paste mode");
        add("tooltip.mekanism_card.memory_card.current_mode", "Current mode: %s");
        add("tooltip.mekanism_card.memory_card.has_tag", "Stored config from %s machines");
        add("tooltip.mekanism_card.memory_card.no_tag", "No config stored");
        add("tooltip.mekanism_card.memory_card.right_click_copy", "Sneak + Right-click machine: Copy config and upgrades");
        add("tooltip.mekanism_card.memory_card.right_click_paste", "Right-click machine: Paste config and upgrades (consumes upgrade cards)");

        add("message.mekanism_card.mode_switched", "Switched to %s");
        add("message.mekanism_card.selection_mode.enabled", "Selection Mode Enabled");
        add("message.mekanism_card.selection_mode.disabled", "Selection Mode Disabled");
        add("message.mekanism_card.selection_point.first", "First corner set: %s");
        add("message.mekanism_card.selection_point.second", "Second corner set: %s");
        add("message.mekanism_card.selection_area.size", "Selection area: %s x %s x %s, total %s blocks");
        add("message.mekanism_card.operation.install", "Installed %s upgrade x%s, affecting %s machines");
        add("message.mekanism_card.operation.remove", "Removed %s upgrade x%s, affecting %s machines");
        add("message.mekanism_card.operation.none", "No changes made");
        add("message.mekanism_card.not_upgradable", "Target block is not an upgradeable machine!");
        add("message.mekanism_card.no_upgrade_in_inventory", "No upgrade module found in inventory!");
        add("message.mekanism_card.no_machines_in_radius", "No other upgradeable machines within %s blocks radius");
        add("message.mekanism_card.no_machines_connected", "No other upgradeable machines nearby");
        add("message.mekanism_card.selection_incomplete", "Selection incomplete, please set two corners first!");

        add("tooltip.mekanism_card.current_upgrade", "Current upgrade: %s");
        add("tooltip.mekanism_card.no_upgrade", "Current upgrade: Not selected");
        add("tooltip.mekanism_card.air_click_switch_mode", "Air right-click: Switch install/remove mode");
        add("tooltip.mekanism_card.sneak_air_click_switch_selection", "Sneak + Air right-click: Switch selection mode");
        add("tooltip.mekanism_card.current_mode", "Current mode: %s | %s");
        add("tooltip.mekanism_card.mode.radius", "Radius Mode");
        add("tooltip.mekanism_card.mode.selection", "Selection Mode");
        add("tooltip.mekanism_card.selection.range", "Selection: %s → %s");
        add("tooltip.mekanism_card.selection.visible", "Selection is visible in world");
        add("tooltip.mekanism_card.selection.first_only", "Selection: First corner set, waiting for second");
        add("tooltip.mekanism_card.selection.none", "Selection: Not set, sneak right-click blocks to set");
        add("tooltip.mekanism_card.selection.execute", "Normal right-click machine: Execute batch operation on selection");
        add("tooltip.mekanism_card.selection.set_point", "Sneak right-click block: Set selection corner");
        add("tooltip.mekanism_card.radius.execute", "Sneak right-click machine: Batch operation on adjacent machines");
        add("tooltip.mekanism_card.radius.no_op", "Normal right-click machine: No operation");
        add("message.mekanism_card.selection_mode.help.first", "Sneak + Right-click block: set first corner");
        add("message.mekanism_card.selection_mode.help.second", "Sneak + Right-click block again: set second corner");
        add("message.mekanism_card.selection_mode.help.execute", "Normal right-click: execute on machine in selection");
        add("message.mekanism_card.selection_outside", "Target block is outside the selection!");
        add("message.mekanism_card.selection_cleared", "Selection cleared - too far away");
        add("message.mekanism_card.selection_must_be_mekanism", "Only Mekanism machines can be selected!");
    }
}

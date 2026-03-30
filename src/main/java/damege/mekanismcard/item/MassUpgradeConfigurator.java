package damege.mekanismcard.item;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import mekanism.api.Upgrade;
import mekanism.common.inventory.slot.UpgradeInventorySlot;
import mekanism.common.item.interfaces.IUpgradeItem;
import mekanism.common.tile.base.TileEntityMekanism;
import mekanism.common.tile.component.TileComponentUpgrade;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class MassUpgradeConfigurator extends Item {
    @Getter
    private Mode currentMode = Mode.INSTALL;
    private static final int DEFAULT_RADIUS = 5;
    private static final int SELECTION_CLEAR_DISTANCE = 5;

    public MassUpgradeConfigurator() {
        super(new Properties().stacksTo(1).rarity(Rarity.UNCOMMON));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide) {
            if (player.isShiftKeyDown()) {
                this.toggleSelectionMode(stack, player);
            } else {
                this.toggleMode(player);
            }
        }
        return InteractionResultHolder.success(stack);
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        return InteractionResult.CONSUME;
    }

    public void handleRadiusMode(Level level, BlockPos pos, Player player) {
        TileComponentUpgrade exampleComp = getUpgradeComponent(level, pos);
        if (exampleComp == null) {
            player.displayClientMessage(Component.translatable("message.mekanism_card.not_upgradable").withStyle(ChatFormatting.RED), true);
            return;
        }

        Upgrade upgradeType = this.getSelectedUpgradeFromInventory(player);
        if (upgradeType == null) {
            player.displayClientMessage(Component.translatable("message.mekanism_card.no_upgrade_in_inventory").withStyle(ChatFormatting.RED), true);
            return;
        }

        Block clickedBlock = level.getBlockState(pos).getBlock();
        List<BlockPos> machines = this.findConnectedMachines(level, pos, clickedBlock);
        if (machines.isEmpty()) {
            player.displayClientMessage(Component.translatable("message.mekanism_card.no_machines_connected").withStyle(ChatFormatting.YELLOW), true);
            return;
        }

        int affectedMachines = 0;
        int totalAmount = 0;
        for (BlockPos machinePos : machines) {
            TileComponentUpgrade comp = this.getUpgradeComponent(level, machinePos);
            if (comp != null) {
                int amount = this.processUpgrade(comp, upgradeType, player, currentMode);
                if (amount > 0) {
                    affectedMachines++;
                    totalAmount += amount;
                }
            }
        }
        this.feedbackDetailed(player, upgradeType, currentMode, affectedMachines, totalAmount);
    }

    public void handleSelectionModeSetPoint(Level level, BlockPos pos, Player player, ItemStack stack) {
        if (this.getUpgradeComponent(level, pos) == null) {
            player.displayClientMessage(Component.translatable("message.mekanism_card.selection_must_be_mekanism").withStyle(ChatFormatting.RED), true);
            return;
        }
        this.setSelectionPoint(stack, pos, player);
    }

    public void handleSelectionModeExecute(Level level, BlockPos pos, Player player, ItemStack stack) {
        BlockPos[] selection = this.getSelection(stack);
        if (selection[0] == null || selection[1] == null) {
            player.displayClientMessage(Component.translatable("message.mekanism_card.selection_incomplete").withStyle(ChatFormatting.RED), true);
            return;
        }

        if (!this.isPosInSelection(pos, selection)) {
            player.displayClientMessage(Component.translatable("message.mekanism_card.selection_outside").withStyle(ChatFormatting.RED), true);
            return;
        }

        TileComponentUpgrade clickedComp = this.getUpgradeComponent(level, pos);
        if (clickedComp == null) {
            player.displayClientMessage(Component.translatable("message.mekanism_card.not_upgradable").withStyle(ChatFormatting.RED), true);
            return;
        }
        this.performBatchOperation(level, selection[0], selection[1], player);
    }

    public boolean checkAndClearSelectionIfTooFar(Level level, Player player, ItemStack stack) {
        BlockPos[] selection = this.getSelection(stack);
        if (selection[0] == null || selection[1] == null) {
            return false;
        }

        BlockPos playerPos = player.getOnPos();
        int minX = Math.min(selection[0].getX(), selection[1].getX()) - SELECTION_CLEAR_DISTANCE;
        int maxX = Math.max(selection[0].getX(), selection[1].getX()) + SELECTION_CLEAR_DISTANCE;
        int minY = Math.min(selection[0].getY(), selection[1].getY()) - SELECTION_CLEAR_DISTANCE;
        int maxY = Math.max(selection[0].getY(), selection[1].getY()) + SELECTION_CLEAR_DISTANCE;
        int minZ = Math.min(selection[0].getZ(), selection[1].getZ()) - SELECTION_CLEAR_DISTANCE;
        int maxZ = Math.max(selection[0].getZ(), selection[1].getZ()) + SELECTION_CLEAR_DISTANCE;
        if (playerPos.getX() < minX || playerPos.getX() > maxX || playerPos.getY() < minY || playerPos.getY() > maxY || playerPos.getZ() < minZ || playerPos.getZ() > maxZ) {
            this.clearSelection(stack);
            player.displayClientMessage(Component.translatable("message.mekanism_card.selection_cleared").withStyle(ChatFormatting.YELLOW), true);
            return true;
        }
        return false;
    }

    private void clearSelection(ItemStack stack) {
        stack.removeTagKey("Pos1");
        stack.removeTagKey("Pos2");
        stack.removeTagKey("Enchantments");
    }

    private boolean isPosInSelection(BlockPos pos, BlockPos[] selection) {
        int minX = Math.min(selection[0].getX(), selection[1].getX());
        int maxX = Math.max(selection[0].getX(), selection[1].getX());
        int minY = Math.min(selection[0].getY(), selection[1].getY());
        int maxY = Math.max(selection[0].getY(), selection[1].getY());
        int minZ = Math.min(selection[0].getZ(), selection[1].getZ());
        int maxZ = Math.max(selection[0].getZ(), selection[1].getZ());
        return pos.getX() >= minX && pos.getX() <= maxX && pos.getY() >= minY && pos.getY() <= maxY && pos.getZ() >= minZ && pos.getZ() <= maxZ;
    }

    public boolean isSelectionModeActive(ItemStack stack) {
        return stack.hasTag() && stack.getTag().getBoolean("SelectionMode");
    }

    public BlockPos[] getSelectionPoints(ItemStack stack) {
        return this.getSelection(stack);
    }

    public Upgrade getSelectedUpgradeFromInventory(Player player) {
        for (ItemStack stack : player.getInventory().items) {
            if (stack.getItem() instanceof IUpgradeItem upgradeItem) {
                return upgradeItem.getUpgradeType(stack);
            }
        }
        return null;
    }

    private void toggleMode(Player player) {
        this.currentMode = (currentMode == Mode.INSTALL) ? Mode.REMOVE : Mode.INSTALL;
        player.displayClientMessage(Component.translatable("message.mekanism_card.mode_switched",
                this.currentMode.getDisplayName()).withStyle(this.currentMode.color), true);
    }

    private void toggleSelectionMode(ItemStack stack, Player player) {
        boolean newMode = !this.isSelectionModeActive(stack);
        CompoundTag tag = stack.getOrCreateTag();
        tag.putBoolean("SelectionMode", newMode);
        if (newMode) {
            tag.remove("Pos1");
            tag.remove("Pos2");
        }

        if (!newMode) {
            stack.removeTagKey("Enchantments");
        }

        player.displayClientMessage(Component.translatable(newMode ? "message.mekanism_card.selection_mode.enabled" : "message.mekanism_card.selection_mode.disabled")
                .withStyle(newMode ? ChatFormatting.GREEN : ChatFormatting.RED), false);
        if (newMode) {
            player.displayClientMessage(Component.translatable("message.mekanism_card.selection_mode.help.first").withStyle(ChatFormatting.GRAY), false);
            player.displayClientMessage(Component.translatable("message.mekanism_card.selection_mode.help.second").withStyle(ChatFormatting.GRAY), false);
            player.displayClientMessage(Component.translatable("message.mekanism_card.selection_mode.help.execute").withStyle(ChatFormatting.GRAY), false);
        }
    }

    private void setSelectionPoint(ItemStack stack, BlockPos pos, Player player) {
        CompoundTag tag = stack.getOrCreateTag();
        if (!tag.contains("Pos1")) {
            tag.put("Pos1", this.newCompound(pos));
            player.displayClientMessage(Component.translatable("message.mekanism_card.selection_point.first", pos.toShortString()).withStyle(ChatFormatting.GREEN), true);
        } else if (!tag.contains("Pos2")) {
            tag.put("Pos2", this.newCompound(pos));
            stack.getTagElement("Enchantments");
            player.displayClientMessage(Component.translatable("message.mekanism_card.selection_point.second", pos.toShortString()).withStyle(ChatFormatting.GREEN), true);

            BlockPos p1 = this.getPosFromTag(tag, "Pos1");
            BlockPos p2 = pos;
            int dx = Math.abs(p1.getX() - p2.getX()) + 1;
            int dy = Math.abs(p1.getY() - p2.getY()) + 1;
            int dz = Math.abs(p1.getZ() - p2.getZ()) + 1;
            player.displayClientMessage(Component.translatable("message.mekanism_card.selection_area.size", dx, dy, dz, dx * dy * dz).withStyle(ChatFormatting.GRAY), true);
        } else {
            tag.remove("Pos1");
            tag.remove("Pos2");
            stack.removeTagKey("Enchantments");
            this.setSelectionPoint(stack, pos, player);
            return;
        }
    }

    private BlockPos[] getSelection(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag == null) {
            return new BlockPos[]{null, null};
        }

        BlockPos p1 = this.getPosFromTag(tag, "Pos1");
        BlockPos p2 = this.getPosFromTag(tag, "Pos2");
        return new BlockPos[]{p1, p2};
    }

    private CompoundTag newCompound(BlockPos pos) {
        CompoundTag tag = new CompoundTag();
        tag.putInt("x", pos.getX());
        tag.putInt("y", pos.getY());
        tag.putInt("z", pos.getZ());
        return tag;
    }

    @Nullable
    private BlockPos getPosFromTag(CompoundTag tag, String key) {
        if (tag.contains(key)) {
            CompoundTag posTag = tag.getCompound(key);
            return new BlockPos(posTag.getInt("x"), posTag.getInt("y"), posTag.getInt("z"));
        }
        return null;
    }

    private void performBatchOperation(Level level, BlockPos p1, BlockPos p2, Player player) {
        int minX = Math.min(p1.getX(), p2.getX());
        int maxX = Math.max(p1.getX(), p2.getX());
        int minY = Math.min(p1.getY(), p2.getY());
        int maxY = Math.max(p1.getY(), p2.getY());
        int minZ = Math.min(p1.getZ(), p2.getZ());
        int maxZ = Math.max(p1.getZ(), p2.getZ());

        Upgrade upgradeType = this.getSelectedUpgradeFromInventory(player);
        if (upgradeType == null) {
            player.displayClientMessage(Component.translatable("message.mekanism_card.no_upgrade_in_inventory").withStyle(ChatFormatting.RED), true);
            return;
        }

        int affectedMachines = 0;
        int totalAmount = 0;
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    TileComponentUpgrade comp = this.getUpgradeComponent(level, pos);
                    if (comp != null) {
                        int amount = this.processUpgrade(comp, upgradeType, player, currentMode);
                        if (amount > 0) {
                            affectedMachines++;
                            totalAmount += amount;
                        }
                    }
                }
            }
        }
        feedbackDetailed(player, upgradeType, currentMode, affectedMachines, totalAmount);
    }

    private void feedbackDetailed(Player player, Upgrade upgradeType, Mode mode, int affectedMachines, int totalAmount) {
        if (totalAmount > 0) {
            String actionKey = mode == Mode.INSTALL ? "message.mekanism_card.operation.install" : "message.mekanism_card.operation.remove";
            player.displayClientMessage(Component.translatable(actionKey, Component.translatable(upgradeType.getTranslationKey()), totalAmount, affectedMachines).withStyle(ChatFormatting.GREEN), true);
        } else {
            player.displayClientMessage(Component.translatable("message.mekanism_card.operation.none").withStyle(ChatFormatting.RED), true);
        }
    }

    private int processUpgrade(TileComponentUpgrade comp, Upgrade upgradeType, Player player, Mode mode) {
        int current = comp.getUpgrades(upgradeType);
        int max = upgradeType.getMax();
        if (mode == Mode.INSTALL) {
            if (current >= max) {
                return 0;
            }

            int toInstall = max - current;
            int available = this.countUpgradeInInventory(player, upgradeType);
            if (available == 0) {
                return 0;
            }
            toInstall = Math.min(toInstall, available);
            if (toInstall <= 0) {
                return 0;
            }
            if (!consumeUpgradeFromInventory(player, upgradeType, toInstall)) {
                return 0;
            }

            return comp.addUpgrades(upgradeType, toInstall);
        } else {
            if (current <= 0) {
                return 0;
            }

            comp.removeUpgrade(upgradeType, true);
            this.handleRemovedUpgrade(comp, player);
            return current;
        }
    }

    private int countUpgradeInInventory(Player player, Upgrade upgradeType) {
        int count = 0;
        for (ItemStack stack : player.getInventory().items) {
            if (stack.getItem() instanceof IUpgradeItem upgradeItem && upgradeItem.getUpgradeType(stack) == upgradeType) {
                count += stack.getCount();
            }
        }
        return count;
    }

    private boolean consumeUpgradeFromInventory(Player player, Upgrade upgradeType, int amount) {
        if (player.getAbilities().instabuild) {
            return true;
        }

        int remaining = amount;
        for (int i = 0; i < player.getInventory().getContainerSize() && remaining > 0; i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.getItem() instanceof IUpgradeItem upgradeItem && upgradeItem.getUpgradeType(stack) == upgradeType) {
                int take = Math.min(remaining, stack.getCount());
                remaining -= take;
                if (stack.isEmpty()) {
                    player.getInventory().setItem(i, ItemStack.EMPTY);
                }
            }
        }
        return remaining == 0;
    }

    private void handleRemovedUpgrade(TileComponentUpgrade comp, Player player) {
        UpgradeInventorySlot outputSlot = comp.getUpgradeOutputSlot();
        ItemStack stack = outputSlot.getStack();
        if (!stack.isEmpty()) {
            if (!player.getInventory().add(stack)) {
                player.drop(stack, false);
            }
            outputSlot.setStack(ItemStack.EMPTY);
        }
    }

    @Nullable
    private TileComponentUpgrade getUpgradeComponent(Level level, BlockPos pos) {
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof TileEntityMekanism tile) {
            return tile.getComponent();
        }
        return null;
    }

    private List<BlockPos> findConnectedMachines(Level level, BlockPos start, Block targetBlock) {
        List<BlockPos> machines = new ArrayList<>();
        Set<BlockPos> visited = new HashSet<>();
        Queue<BlockPos> queue = new ArrayDeque<>();
        queue.add(start);
        visited.add(start);

        while (!queue.isEmpty()) {
            BlockPos current = queue.poll();
            if (this.getUpgradeComponent(level, current) != null) {
                machines.add(current);
            }

            for (Direction direction : Direction.values()) {
                BlockPos neighbor = current.relative(direction);
                if (!visited.contains(neighbor)) {
                    Block neighborBlock = level.getBlockState(neighbor).getBlock();
                    if (neighborBlock == targetBlock && this.getUpgradeComponent(level, neighbor) != null) {
                        visited.add(neighbor);
                        queue.add(neighbor);
                    }
                }
            }
        }
        return machines;
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag isAdvanced) {
        Player player = Minecraft.getInstance().player;
        Upgrade upgrade = null;
        if (player != null) {
            upgrade = this.getSelectedUpgradeFromInventory(player);
        }

        if (upgrade != null) {
            tooltip.add(Component.translatable("tooltip.mekanism_card.current_upgrade", Component.translatable(upgrade.getTranslationKey())).withStyle(ChatFormatting.DARK_AQUA));
        } else {
            tooltip.add(Component.translatable("tooltip.mekanism_card.no_upgrade").withStyle(ChatFormatting.DARK_GREEN));
        }

        tooltip.add(Component.translatable("tooltip.mekanism_card.air_click_switch_mode").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.mekanism_card.sneak_air_click_switch_selection").withStyle(ChatFormatting.DARK_GREEN));

        boolean selectionMode = isSelectionModeActive(stack);
        String modeKey = selectionMode ? "tooltip.mekanism_card.mode.selection" : "tooltip.mekanism_card.mode.radius";
        tooltip.add(Component.translatable("tooltip.mekanism_card.current_mode", Component.translatable(modeKey), currentMode.getDisplayName()).withStyle(selectionMode ? ChatFormatting.AQUA : ChatFormatting.GOLD));

        if (selectionMode) {
            BlockPos[] sel = this.getSelection(stack);
            if (sel[0] != null && sel[1] != null) {
                tooltip.add(Component.translatable("tooltip.mekanism_card.selection.range", sel[0].toShortString(), sel[1].toShortString()).withStyle(ChatFormatting.GRAY));
                tooltip.add(Component.translatable("tooltip.mekanism_card.selection.visible").withStyle(ChatFormatting.GRAY));
            } else if (sel[0] != null) {
                tooltip.add(Component.translatable("tooltip.mekanism_card.selection.first_only").withStyle(ChatFormatting.GRAY));
            } else {
                tooltip.add(Component.translatable("tooltip.mekanism_card.selection.none").withStyle(ChatFormatting.GRAY));
            }

            tooltip.add(Component.translatable("tooltip.mekanism_card.selection.execute").withStyle(ChatFormatting.GRAY));
            tooltip.add(Component.translatable("tooltip.mekanism_card.selection.set_point").withStyle(ChatFormatting.GRAY));
        } else {
            tooltip.add(Component.translatable("tooltip.mekanism_card.radius.execute", DEFAULT_RADIUS).withStyle(ChatFormatting.GRAY));
            tooltip.add(Component.translatable("tooltip.mekanism_card.radius.no_op").withStyle(ChatFormatting.GRAY));
        }
        super.appendHoverText(stack, level, tooltip, isAdvanced);
    }

    @RequiredArgsConstructor
    public enum Mode {
        INSTALL("mekanism_card.mode.install", ChatFormatting.GREEN),
        REMOVE("mekanism_card.mode.remove", ChatFormatting.RED);

        public final String translationKey;
        public final ChatFormatting color;

        public Component getDisplayName() {
            return Component.translatable(this.translationKey);
        }
    }
}

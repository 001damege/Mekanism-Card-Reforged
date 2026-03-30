package damege.mekanismcard.item;

import mekanism.api.IConfigCardAccess;
import mekanism.api.NBTConstants;
import mekanism.api.Upgrade;
import mekanism.common.item.interfaces.IUpgradeItem;
import mekanism.common.tile.base.TileEntityMekanism;
import mekanism.common.tile.component.TileComponentUpgrade;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class MemoryCard extends Item {
    public MemoryCard() {
        super(new Properties().stacksTo(1).rarity(Rarity.UNCOMMON));
    }

    public static boolean hasTag(ItemStack stack) {
        return stack.hasTag() && Objects.requireNonNull(stack.getTag()).contains("MachineTag");
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag isAdvanced) {
        tooltip.add(Component.translatable("tooltip.mekanism_card.memory_card.right_click_copy").withStyle(ChatFormatting.DARK_GREEN));
        tooltip.add(Component.translatable("tooltip.mekanism_card.memory_card.right_click_paste").withStyle(ChatFormatting.DARK_AQUA));
        tooltip.add(Component.translatable("tooltip.mekanism_card.memory_card.sneak_air_click_clear").withStyle(ChatFormatting.RED));

        if (stack.hasTag() && Objects.requireNonNull(stack.getTag()).contains("MachineTag")) {
            CompoundTag tag = stack.getTag().getCompound("MachineTag");
            String blockType = tag.getString("SourceBlockType");
            if (!blockType.isEmpty()) {
                ResourceLocation location = ResourceLocation.tryParse(blockType);
                Block block = ForgeRegistries.BLOCKS.getValue(location);
                if (block != Blocks.AIR && block != null) {
                    tooltip.add(Component.translatable("tooltip.mekanism_card.memory_card.has_tag", block.getName().getString()).withStyle(ChatFormatting.GREEN));
                    return;
                }
            }
        }
        tooltip.add(Component.translatable("tooltip.mekanism_card.memory_card.no_tag").withStyle(ChatFormatting.RED));
    }

    public static void handleCopyStatic(Level level, BlockPos pos, Player player, ItemStack stack) {
        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof IConfigCardAccess)) {
            player.displayClientMessage(Component.translatable("message.mekanism_card.memory_card.not_mekanism").withStyle(ChatFormatting.RED), false);
            return;
        }

        IConfigCardAccess machine = (IConfigCardAccess) be;
        Block targetBlock = level.getBlockState(pos).getBlock();
        CompoundTag tag = new CompoundTag();
        ListTag upgradeList = new ListTag();
        if (be instanceof TileEntityMekanism tile) {
            TileComponentUpgrade comp = tile.getComponent();
            if (comp != null) {
                for (Upgrade upgrade : Upgrade.values()) {
                    int upgradeLevel = comp.getUpgrades(upgrade);
                    if (upgradeLevel > 0) {
                        CompoundTag upgradeTag = new CompoundTag();
                        upgradeTag.putInt(NBTConstants.TYPE, upgrade.ordinal());
                        upgradeTag.putInt(NBTConstants.AMOUNT, upgradeLevel);
                        upgradeList.add(upgradeTag);
                    }
                }
            }
        }

        tag.put(NBTConstants.UPGRADES, upgradeList);
        CompoundTag configTag = machine.getConfigurationData(player);
        if (configTag != null && !configTag.isEmpty()) {
            tag.put("ConfigTag", configTag);
            tag.putString("BlockType", ForgeRegistries.BLOCKS.getDelegate(targetBlock).toString());
        }

        tag.putInt("MachineCount", 1);
        tag.putString("SourceBlockType", ForgeRegistries.BLOCKS.getDelegate(targetBlock).toString());
        CompoundTag data = stack.getOrCreateTag();
        data.put("MachineTag", tag);
        player.displayClientMessage(Component.translatable("message.mekanism_card.memory_card.copied", 1, upgradeList.size()).withStyle(ChatFormatting.GREEN), false);
    }

    public static void handleClearStatic(Player player, ItemStack stack) {
        stack.removeTagKey("MachineTag");
        player.displayClientMessage(Component.translatable("message.mekanism_card.memory_card.cleared").withStyle(ChatFormatting.YELLOW), true);
    }

    public static void handlePasteStatic(Level level, BlockPos pos, Player player, ItemStack stack) {
        if (!stack.hasTag() || !stack.getTag().contains("MachineTag")) {
            player.displayClientMessage(Component.translatable("message.mekanism_card.memory_card.no_tag").withStyle(ChatFormatting.RED), true);
            return;
        }

        if (!(level.getBlockEntity(pos) instanceof IConfigCardAccess)) {
            player.displayClientMessage(Component.translatable("message.mekanism_card.memory_card.not_mekanism").withStyle(ChatFormatting.RED), true);
            return;
        }

        CompoundTag machineTag = stack.getTag().getCompound("MachineTag");
        String sourceBlockType = machineTag.getString("SourceBlockType");
        Block targetBlock = level.getBlockState(pos).getBlock();
        String targetBlockType = ForgeRegistries.BLOCKS.getDelegate(targetBlock).toString();
        if (!sourceBlockType.isEmpty() && !sourceBlockType.equals(targetBlockType)) {
            player.displayClientMessage(Component.translatable("message.mekanism_card.memory_card.type_mismatch").withStyle(ChatFormatting.RED),true);
            return;
        }

        List<BlockPos> connectedMachines = findConnectedMachines(level, pos, targetBlock);
        if (connectedMachines.isEmpty()) {
            player.displayClientMessage(Component.translatable("message.mekanism_card.memory_card.paste_failed").withStyle(ChatFormatting.YELLOW), true);
            return;
        }

        Map<Upgrade, Integer> perMachineUpgrade = new EnumMap<>(Upgrade.class);
        if (machineTag.contains(NBTConstants.UPGRADES)) {
            ListTag upgradeList = machineTag.getList(NBTConstants.UPGRADES, Tag.TAG_COMPOUND);
            int machineCount = connectedMachines.size();
            int originalMachineCount = machineTag.getInt("MachineCount");
            if (originalMachineCount <= 0) {
                originalMachineCount = 1;
            }

            for (int i = 0; i < upgradeList.size(); i++) {
                CompoundTag upgradeTag = upgradeList.getCompound(i);
                int typeOrdinal = upgradeTag.getInt(NBTConstants.TYPE);
                int totalAmount = upgradeTag.getInt(NBTConstants.AMOUNT);
                if (typeOrdinal >= 0 && typeOrdinal < Upgrade.values().length && totalAmount > 0 && machineCount > 0) {
                    Upgrade upgrade = Upgrade.values()[typeOrdinal];
                    int amountPerMachine = (totalAmount * machineCount) / originalMachineCount;
                    amountPerMachine = Math.min(amountPerMachine, upgrade.getMax());
                    perMachineUpgrade.put(upgrade, amountPerMachine);
                }
            }
        }

        boolean isCreative = player.getAbilities().instabuild;
        Map<Upgrade, Integer> neededUpgrades = new EnumMap<>(Upgrade.class);
        if (!isCreative && machineTag.contains(NBTConstants.UPGRADES)) {
            for (Map.Entry<Upgrade, Integer> entry : perMachineUpgrade.entrySet()) {
                int totalNeeded = entry.getValue() * connectedMachines.size();
                if (totalNeeded > 0) {
                    neededUpgrades.put(entry.getKey(), totalNeeded);
                }
            }

            if (!neededUpgrades.isEmpty() && !hasEnoughUpgrades(player, neededUpgrades)) {
                player.displayClientMessage(Component.translatable("message.mekanism_card.memory_card.not_enough_upgrades").withStyle(ChatFormatting.RED), true);
                return;
            }
        }

        Map<Upgrade, Integer> consumedUpgrades = new EnumMap<>(Upgrade.class);
        int machinesAffected = 0;
        for (BlockPos machinePos : connectedMachines) {
            BlockEntity be = level.getBlockEntity(machinePos);
            if (!(be instanceof TileEntityMekanism machine)) {
                continue;
            }

            boolean affected = false;
            if (machineTag.contains(NBTConstants.UPGRADES)) {
                TileComponentUpgrade upgradeComp = machine.getComponent();
                if (upgradeComp != null) {
                    for (Map.Entry<Upgrade, Integer> entry : perMachineUpgrade.entrySet()) {
                        Upgrade upgrade = entry.getKey();
                        int targetLevel = entry.getValue();
                        int current = upgradeComp.getUpgrades(upgrade);
                        int toAdd = Math.min(targetLevel - current, upgrade.getMax() - current);
                        if (toAdd > 0) {
                            int added = upgradeComp.addUpgrades(upgrade, toAdd);
                            if (added > 0) {
                                affected = true;
                                if (!isCreative) {
                                    consumedUpgrades.put(upgrade, consumedUpgrades.getOrDefault(upgrade, 0) + added);
                                }
                            }
                        }
                    }
                }
            }

            if (machineTag.contains("ConfigTag") && be instanceof IConfigCardAccess configAccess) {
                CompoundTag configTag = machineTag.getCompound("ConfigTag");
                configAccess.setConfigurationData(player, configTag);
                configAccess.configurationDataSet();
                affected = true;
            }
            if (affected) {
                machinesAffected++;
            }
        }

        if (!isCreative && !consumedUpgrades.isEmpty()) {
            consumeUpgradeCardsByType(player, consumedUpgrades);
        }
        if (machinesAffected > 0) {
            if (isCreative) {
                player.displayClientMessage(Component.translatable("message.mekanism_card.memory_card.pasted_creative").withStyle(ChatFormatting.GREEN), true);
            } else {
                player.displayClientMessage(Component.translatable("message.mekanism_card.memory_card.pasted").withStyle(ChatFormatting.GREEN), true);
            }
        } else {
            player.displayClientMessage(Component.translatable("message.mekanism_card.memory_card.paste_failed").withStyle(ChatFormatting.YELLOW), true);
        }
    }

    private static int countTotalUpgradeCards(Player player) {
        int count = 0;
        for (ItemStack stack : player.getInventory().items) {
            if (stack.getItem() instanceof IUpgradeItem upgradeItem) {
                Upgrade upgradeType = upgradeItem.getUpgradeType(stack);
                count += stack.getCount();
            }
        }
        return count;
    }

    private static boolean hasEnoughUpgrades(Player player, Map<Upgrade, Integer> required) {
        Map<Upgrade, Integer> available = new EnumMap<>(Upgrade.class);
        for (ItemStack stack : player.getInventory().items) {
            if (stack.getItem() instanceof IUpgradeItem upgradeItem) {
                Upgrade upgradeType = upgradeItem.getUpgradeType(stack);
                available.put(upgradeType, available.getOrDefault(upgradeType, 0) + stack.getCount());
            }
        }

        for (Map.Entry<Upgrade, Integer> entry : required.entrySet()) {
            if (available.getOrDefault(entry.getKey(), 0) < entry.getValue()) {
                return false;
            }
        }
        return true;
    }

    private static void consumeUpgradeCardsByType(Player player, Map<Upgrade, Integer> required) {
        for (Map.Entry<Upgrade, Integer> entry : required.entrySet()) {
            Upgrade neededType = entry.getKey();
            int remaining = entry.getValue();
            for (int i = 0; i < player.getInventory().getContainerSize() && remaining > 0; i++) {
                ItemStack stack = player.getInventory().getItem(i);
                if (stack.getItem() instanceof IUpgradeItem upgradeItem) {
                    Upgrade cardType = upgradeItem.getUpgradeType(stack);
                    if (cardType == neededType) {
                        int take = Math.min(remaining, stack.getCount());
                        stack.shrink(take);
                        remaining -= take;
                        if (stack.isEmpty()) {
                            player.getInventory().setItem(i, ItemStack.EMPTY);
                        }
                    }
                }
            }
        }
    }

    private static List<BlockPos> findConnectedMachines(Level level, BlockPos start, Block targetBlock) {
        Set<BlockPos> machines = new HashSet<>();
        Set<BlockPos> visited = new HashSet<>();
        Queue<BlockPos> queue = new ArrayDeque<>();
        queue.add(start);
        visited.add(start);

        while (!queue.isEmpty()) {
            BlockPos current = queue.poll();
            if (level.getBlockEntity(current) instanceof TileEntityMekanism && level.getBlockState(current).getBlock() == targetBlock) {
                machines.add(current);
            }

            for (Direction direction : Direction.values()) {
                BlockPos neighbor = current.relative(direction);
                if (!visited.contains(neighbor)) {
                    if (level.getBlockState(neighbor).getBlock() == targetBlock && level.getBlockEntity(neighbor) instanceof TileEntityMekanism) {
                        visited.add(neighbor);
                        queue.add(neighbor);
                    }
                }
            }
        }
        return new ArrayList<>(machines);
    }

    private static String upgradeKeyToSimpleName(Upgrade upgrade) {
        return switch (upgrade) {
            case SPEED -> "速度升级";
            case ENERGY -> "能源升级";
            case FILTER -> "过滤升级";
            case GAS -> "化学升级";
            case MUFFLING -> "降噪升级";
            case ANCHOR -> "锚点升级";
            case STONE_GENERATOR -> "石头生成升级";
            default -> upgrade.getRawName();
        };
    }
}

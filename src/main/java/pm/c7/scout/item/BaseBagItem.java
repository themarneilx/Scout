package pm.c7.scout.item;

import dev.emi.trinkets.api.SlotReference;
import dev.emi.trinkets.api.TrinketItem;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.item.tooltip.TooltipData;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import pm.c7.scout.ScoutNetworking;
import pm.c7.scout.ScoutScreenHandler;
import pm.c7.scout.ScoutUtil;
import pm.c7.scout.screen.BagSlot;

import java.util.List;
import java.util.Optional;

import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;

public class BaseBagItem extends TrinketItem {
	private static final String ITEMS_KEY = "Items";

	private final int slots;
	private final BagType type;

	public BaseBagItem(Settings settings, int slots, BagType type) {
		super(settings);

		if (type == BagType.SATCHEL && slots > ScoutUtil.MAX_SATCHEL_SLOTS) {
			throw new IllegalArgumentException("Satchel has too many slots.");
		}
		if (type == BagType.POUCH && slots > ScoutUtil.MAX_POUCH_SLOTS) {
			throw new IllegalArgumentException("Pouch has too many slots.");
		}

		this.slots = slots;
		this.type = type;
	}

	@Override
	public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
		ScoutUtil.LOGGER.info("Scout: Attempting to use bag item: " + this);
		boolean equipped = TrinketItem.equipItem(user, user.getStackInHand(hand));
		ScoutUtil.LOGGER.info("Scout: equipItem returned: " + equipped);
		
		if (equipped) {
			return TypedActionResult.success(user.getStackInHand(hand));
		}
		return super.use(world, user, hand);
	}

	@Override
	public boolean canEquip(ItemStack stack, SlotReference slot, LivingEntity entity) {
		ScoutUtil.LOGGER.info("Scout: canEquip checking for " + stack.getItem());
		Item item = stack.getItem();

		ItemStack slotStack = slot.inventory().getStack(slot.index());
		Item slotItem = slotStack.getItem();

		boolean can;
		if (slotItem instanceof BaseBagItem) {
			if (((BaseBagItem) item).getType() == BagType.SATCHEL) {
				if (((BaseBagItem) slotItem).getType() == BagType.SATCHEL) {
					can = true;
				} else {
					can = ScoutUtil.findBagItem((PlayerEntity) entity, BagType.SATCHEL, false).isEmpty();
				}
			} else if (((BaseBagItem) item).getType() == BagType.POUCH) {
				if (((BaseBagItem) slotItem).getType() == BagType.POUCH) {
					can = true;
				} else {
					can = ScoutUtil.findBagItem((PlayerEntity) entity, BagType.POUCH, true).isEmpty();
				}
			} else {
				can = false;
			}
		} else {
			if (((BaseBagItem) item).getType() == BagType.SATCHEL) {
				can = ScoutUtil.findBagItem((PlayerEntity) entity, BagType.SATCHEL, false).isEmpty();
			} else if (((BaseBagItem) item).getType() == BagType.POUCH) {
				can = ScoutUtil.findBagItem((PlayerEntity) entity, BagType.POUCH, true).isEmpty();
			} else {
				can = true; // Should not happen for BaseBagItem types?
			}
		}
		ScoutUtil.LOGGER.info("Scout: canEquip returning: " + can);
		return can;
	}

	public int getSlotCount() {
		return this.slots;
	}

	public BagType getType() {
		return this.type;
	}

	@Override
	public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
		super.appendTooltip(stack, context, tooltip, type);
		tooltip.add(Text.translatable("tooltip.scout.slots", Text.literal(String.valueOf(this.slots)).formatted(Formatting.BLUE)).formatted(Formatting.GRAY));
	}

	public Inventory getInventory(ItemStack stack) {
		ScoutUtil.ItemStackBagInventory inventory = new ScoutUtil.ItemStackBagInventory(this.slots, stack);
		inventory.setLoading(true);
		try {
			ScoutUtil.loadInventory(stack, inventory);
		} finally {
			inventory.setLoading(false);
		}

		return inventory;
	}

	@Override
	public Optional<TooltipData> getTooltipData(ItemStack stack) {
		DefaultedList<ItemStack> stacks = DefaultedList.of();
		Inventory inventory = getInventory(stack);

		for (int i = 0; i < slots; i++) {
			stacks.add(inventory.getStack(i));
		}

		if (stacks.stream().allMatch(ItemStack::isEmpty)) return Optional.empty();

		return Optional.of(new BagTooltipData(stacks, slots));
	}

	@Override
	public void onEquip(ItemStack stack, SlotReference slotRef, LivingEntity entity) {
		if (entity instanceof PlayerEntity player)
			updateSlots(player);
	}

	@Override
	public void onUnequip(ItemStack stack, SlotReference slotRef, LivingEntity entity) {
		if (entity instanceof PlayerEntity player)
			updateSlots(player);
	}

	private void updateSlots(PlayerEntity player) {
		if (player instanceof ServerPlayerEntity serverPlayer) {
			ScoutUtil.refreshSlots(serverPlayer);
		}
	}

	@Override
	public void inventoryTick(ItemStack stack, World world, Entity entity, int slot, boolean selected) {
		var inv = getInventory(stack);

		for (int i = 0; i < inv.size(); i++) {
			var invStack = inv.getStack(i);
			invStack.inventoryTick(world, entity, i, false);
		}
	}

	@Override
	public void tick(ItemStack stack, SlotReference slot, LivingEntity entity) {
		var inv = getInventory(stack);

		for (int i = 0; i < inv.size(); i++) {
			var invStack = inv.getStack(i);
			invStack.inventoryTick(entity.getWorld(), entity, i, false);
		}
	}

	public enum BagType {
		SATCHEL,
		POUCH
	}
}

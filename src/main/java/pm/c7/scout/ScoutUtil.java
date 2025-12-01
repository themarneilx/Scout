package pm.c7.scout;

import dev.emi.trinkets.api.SlotReference;
import dev.emi.trinkets.api.TrinketComponent;
import dev.emi.trinkets.api.TrinketsApi;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ContainerComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.util.Identifier;
import net.minecraft.util.Pair;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.MathHelper;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pm.c7.scout.item.BaseBagItem;
import pm.c7.scout.item.BaseBagItem.BagType;
import pm.c7.scout.screen.BagSlot;

import java.util.Optional;

public class ScoutUtil {
	public static final Logger LOGGER = LoggerFactory.getLogger("Scout");
	public static final String MOD_ID = "scout";
	public static final Identifier SLOT_TEXTURE = Identifier.of(MOD_ID, "textures/gui/slots.png");

	public static final TagKey<Item> TAG_ITEM_BLACKLIST = TagKey.of(RegistryKeys.ITEM, Identifier.of(MOD_ID, "blacklist"));

	public static final int MAX_SATCHEL_SLOTS = 18;
	public static final int MAX_POUCH_SLOTS = 6;
	public static final int TOTAL_SLOTS = MAX_SATCHEL_SLOTS + MAX_POUCH_SLOTS + MAX_POUCH_SLOTS;

	public static final int SATCHEL_SLOT_START = -1100;
	public static final int LEFT_POUCH_SLOT_START = SATCHEL_SLOT_START - MAX_SATCHEL_SLOTS;
	public static final int RIGHT_POUCH_SLOT_START = LEFT_POUCH_SLOT_START - MAX_POUCH_SLOTS;
	public static final int BAG_SLOTS_END = RIGHT_POUCH_SLOT_START - MAX_POUCH_SLOTS;
	public static final int SCOUT_MASK = 1000000000;

	public static Pair<SlotReference, ItemStack> findBagRef(PlayerEntity player, BaseBagItem.BagType type, boolean right) {
		Pair<SlotReference, ItemStack> targetPair = new Pair<>(null, ItemStack.EMPTY);

		boolean hasFirstPouch = false;
		Optional<TrinketComponent> _component = TrinketsApi.getTrinketComponent(player);
		if (_component.isPresent()) {
			TrinketComponent component = _component.get();
			for (Pair<SlotReference, ItemStack> pair : component.getAllEquipped()) {
				ItemStack slotStack = pair.getRight();

				if (slotStack.getItem() instanceof BaseBagItem bagItem) {
					if (bagItem.getType() == type) {
						if (type == BagType.POUCH) {
							if (right && !hasFirstPouch) {
								hasFirstPouch = true;
							} else {
								targetPair = pair;
								break;
							}
						} else {
							targetPair = pair;
							break;
						}
					}
				}
			}
		}

		return targetPair;
	}

	public static ItemStack findBagItem(PlayerEntity player, BaseBagItem.BagType type, boolean right) {
		return findBagRef(player, type, right).getRight();
	}

	public static class BagInventory extends net.minecraft.inventory.SimpleInventory {
		private final SlotReference ref;
		private boolean loading = false;

		public BagInventory(int size, SlotReference ref) {
			super(size);
			this.ref = ref;
		}

		public void setLoading(boolean loading) {
			this.loading = loading;
		}

		@Override
		public void markDirty() {
			if (loading) return;

			if (ref != null) {
				ItemStack currentStack = ref.inventory().getStack(ref.index());
				if (!currentStack.isEmpty()) {
					ScoutUtil.saveInventory(currentStack, this);
					ref.inventory().markDirty();
				}
			}
			super.markDirty();
		}
	}

	public static class ItemStackBagInventory extends net.minecraft.inventory.SimpleInventory {
		private final ItemStack stack;
		private boolean loading = false;

		public ItemStackBagInventory(int size, ItemStack stack) {
			super(size);
			this.stack = stack;
		}

		public void setLoading(boolean loading) {
			this.loading = loading;
		}

		@Override
		public void markDirty() {
			if (loading) return;
			ScoutUtil.saveInventory(stack, this);
			super.markDirty();
		}
	}

	public static void refreshSlots(net.minecraft.server.network.ServerPlayerEntity player) {
		ScoutScreenHandler handler = (ScoutScreenHandler) player.playerScreenHandler;

		Pair<SlotReference, ItemStack> satchelPair = findBagRef(player, BaseBagItem.BagType.SATCHEL, false);
		ItemStack satchelStack = satchelPair.getRight();

		DefaultedList<BagSlot> satchelSlots = handler.scout$getSatchelSlots();

		for (int i = 0; i < MAX_SATCHEL_SLOTS; i++) {
			BagSlot slot = satchelSlots.get(i);
			slot.setInventory(null);
			slot.setEnabled(false);
		}
		if (!satchelStack.isEmpty()) {
			BaseBagItem satchelItem = (BaseBagItem) satchelStack.getItem();
			BagInventory satchelInv = new BagInventory(satchelItem.getSlotCount(), satchelPair.getLeft());
			satchelInv.setLoading(true);
			try {
				loadInventory(satchelStack, satchelInv);
			} finally {
				satchelInv.setLoading(false);
			}

			for (int i = 0; i < satchelItem.getSlotCount(); i++) {
				BagSlot slot = satchelSlots.get(i);
				slot.setInventory(satchelInv);
				slot.setEnabled(true);
			}
		}

		Pair<SlotReference, ItemStack> leftPouchPair = findBagRef(player, BaseBagItem.BagType.POUCH, false);
		ItemStack leftPouchStack = leftPouchPair.getRight();
		DefaultedList<BagSlot> leftPouchSlots = handler.scout$getLeftPouchSlots();

		for (int i = 0; i < MAX_POUCH_SLOTS; i++) {
			BagSlot slot = leftPouchSlots.get(i);
			slot.setInventory(null);
			slot.setEnabled(false);
		}
		if (!leftPouchStack.isEmpty()) {
			BaseBagItem leftPouchItem = (BaseBagItem) leftPouchStack.getItem();
			BagInventory leftPouchInv = new BagInventory(leftPouchItem.getSlotCount(), leftPouchPair.getLeft());
			leftPouchInv.setLoading(true);
			try {
				loadInventory(leftPouchStack, leftPouchInv);
			} finally {
				leftPouchInv.setLoading(false);
			}

			for (int i = 0; i < leftPouchItem.getSlotCount(); i++) {
				BagSlot slot = leftPouchSlots.get(i);
				slot.setInventory(leftPouchInv);
				slot.setEnabled(true);
			}
		}

		Pair<SlotReference, ItemStack> rightPouchPair = findBagRef(player, BaseBagItem.BagType.POUCH, true);
		ItemStack rightPouchStack = rightPouchPair.getRight();
		DefaultedList<BagSlot> rightPouchSlots = handler.scout$getRightPouchSlots();

		for (int i = 0; i < MAX_POUCH_SLOTS; i++) {
			BagSlot slot = rightPouchSlots.get(i);
			slot.setInventory(null);
			slot.setEnabled(false);
		}
		if (!rightPouchStack.isEmpty()) {
			BaseBagItem rightPouchItem = (BaseBagItem) rightPouchStack.getItem();
			BagInventory rightPouchInv = new BagInventory(rightPouchItem.getSlotCount(), rightPouchPair.getLeft());
			rightPouchInv.setLoading(true);
			try {
				loadInventory(rightPouchStack, rightPouchInv);
			} finally {
				rightPouchInv.setLoading(false);
			}

			for (int i = 0; i < rightPouchItem.getSlotCount(); i++) {
				BagSlot slot = rightPouchSlots.get(i);
				slot.setInventory(rightPouchInv);
				slot.setEnabled(true);
			}
		}

		net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.send(player, new ScoutNetworking.EnableSlotsPayload(satchelStack, leftPouchStack, rightPouchStack));
	}

	public static void saveInventory(ItemStack bag, Inventory inventory) {
		DefaultedList<ItemStack> stacks = DefaultedList.ofSize(inventory.size(), ItemStack.EMPTY);
		for(int i = 0; i < inventory.size(); i++) {
			stacks.set(i, inventory.getStack(i));
		}
		bag.set(DataComponentTypes.CONTAINER, ContainerComponent.fromStacks(stacks));
	}

	public static void loadInventory(ItemStack bag, Inventory inventory) {
		inventory.clear();
		var component = bag.get(DataComponentTypes.CONTAINER);
		if (component != null) {
			int i = 0;
			for (ItemStack stack : component.stream().toList()) {
				if (i < inventory.size()) {
					inventory.setStack(i, stack);
				}
				i++;
			}
		}
	}

	public static boolean isBagSlot(int slot) {
		return slot <= SATCHEL_SLOT_START && slot > BAG_SLOTS_END;
	}

	public static @Nullable Slot getBagSlot(int slot, PlayerScreenHandler playerScreenHandler) {
		var scoutScreenHandler = (ScoutScreenHandler) playerScreenHandler;
		if (slot <= SATCHEL_SLOT_START && slot > LEFT_POUCH_SLOT_START) {
			int realSlot = MathHelper.abs(slot - SATCHEL_SLOT_START);
			var slots = scoutScreenHandler.scout$getSatchelSlots();

			return slots.get(realSlot);
		} else if (slot <= LEFT_POUCH_SLOT_START && slot > RIGHT_POUCH_SLOT_START) {
			int realSlot = MathHelper.abs(slot - LEFT_POUCH_SLOT_START);
			var slots = scoutScreenHandler.scout$getLeftPouchSlots();

			return slots.get(realSlot);
		} else if (slot <= RIGHT_POUCH_SLOT_START && slot > BAG_SLOTS_END) {
			int realSlot = MathHelper.abs(slot - RIGHT_POUCH_SLOT_START);
			var slots = scoutScreenHandler.scout$getRightPouchSlots();

			return slots.get(realSlot);
		} else {
			return null;
		}
	}

	public static DefaultedList<Slot> getAllBagSlots(PlayerScreenHandler playerScreenHandler) {
		var scoutScreenHandler = (ScoutScreenHandler) playerScreenHandler;
		DefaultedList<Slot> out = DefaultedList.ofSize(TOTAL_SLOTS);
		out.addAll(scoutScreenHandler.scout$getSatchelSlots());
		out.addAll(scoutScreenHandler.scout$getLeftPouchSlots());
		out.addAll(scoutScreenHandler.scout$getRightPouchSlots());
		return out;
	}
}
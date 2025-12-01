package pm.c7.scout.mixin;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.collection.DefaultedList;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pm.c7.scout.ScoutUtil;

@Mixin(value = ScreenHandler.class, priority = 950)
public abstract class ScreenHandlerMixin {

	@ModifyVariable(method = "internalOnSlotClick", at = @At("HEAD"), ordinal = 0, argsOnly = true)
	private int scout$maskSlotIndex(int slotIndex) {
		if (ScoutUtil.isBagSlot(slotIndex)) {
			return slotIndex + ScoutUtil.SCOUT_MASK;
		}
		return slotIndex;
	}

	@Inject(method = "internalOnSlotClick", at = @At("HEAD"))
	public void scout$fixDoubleClick(int slotIndex, int button, SlotActionType actionType, PlayerEntity player, CallbackInfo ci) {
		if (actionType == SlotActionType.PICKUP_ALL) {
			int realIndex = slotIndex;
			// Since ModifyVariable runs before Inject, slotIndex might already be masked if it was a bag slot.
			if (realIndex >= ScoutUtil.SCOUT_MASK - 2000) { // Use safe check
				realIndex = realIndex - ScoutUtil.SCOUT_MASK;
			}

			Slot slot3 = null;
			if (ScoutUtil.isBagSlot(realIndex)) {
				if ((Object)this instanceof net.minecraft.screen.PlayerScreenHandler playerHandler) {
					slot3 = ScoutUtil.getBagSlot(realIndex, playerHandler);
				}
			} else if (realIndex >= 0 && realIndex < this.slots.size()) {
				slot3 = this.slots.get(realIndex);
			}

			if (slot3 != null) {
				ItemStack cursorStack = this.getCursorStack();
				if (!cursorStack.isEmpty() && (!slot3.hasStack() || !slot3.canTakeItems(player))) {
					var slots = ScoutUtil.getAllBagSlots(player.playerScreenHandler);
					var k = button == 0 ? 0 : ScoutUtil.TOTAL_SLOTS - 1;
					var o = button == 0 ? 1 : -1;

					for (int n = 0; n < 2; ++n) {
						for (int p = k; p >= 0 && p < slots.size() && cursorStack.getCount() < cursorStack.getMaxCount(); p += o) {
							Slot slot4 = slots.get(p);
							if (slot4.hasStack() && canInsertItemIntoSlot(slot4, cursorStack, true) && slot4.canTakeItems(player) && this.canInsertIntoSlot(cursorStack, slot4)) {
								ItemStack itemStack6 = slot4.getStack();
								if (n != 0 || itemStack6.getCount() != itemStack6.getMaxCount()) {
									ItemStack itemStack7 = slot4.takeStackRange(itemStack6.getCount(), cursorStack.getMaxCount() - cursorStack.getCount(), player);
									cursorStack.increment(itemStack7.getCount());
								}
							}
						}
					}
				}
			}
		}
	}

	@Redirect(method = "internalOnSlotClick", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/collection/DefaultedList;get(I)Ljava/lang/Object;"))
	public Object scout$redirectGetSlot(DefaultedList<Slot> list, int index, int slotIndex, int button) {
		if (index >= ScoutUtil.SCOUT_MASK - 2000) {
			int realIndex = index - ScoutUtil.SCOUT_MASK;
			if (ScoutUtil.isBagSlot(realIndex)) {
				// ScoutUtil.LOGGER.info("Scout: Intercepted slot click at masked index " + index + " (real: " + realIndex + ")");
				ScreenHandler self = (ScreenHandler) (Object) this;
				if (self instanceof pm.c7.scout.ScoutScreenHandler scoutHandler) {
					return ScoutUtil.getBagSlot(realIndex, (net.minecraft.screen.PlayerScreenHandler) self);
				}
			}
		}
		return list.get(index);
	}

	@Redirect(method = "internalOnSlotClick", at = @At(value = "INVOKE", target = "Lnet/minecraft/screen/ScreenHandler;quickMove(Lnet/minecraft/entity/player/PlayerEntity;I)Lnet/minecraft/item/ItemStack;"))
	public ItemStack scout$redirectQuickMove(ScreenHandler instance, PlayerEntity player, int index) {
		if (index >= ScoutUtil.SCOUT_MASK - 2000) {
			int realIndex = index - ScoutUtil.SCOUT_MASK;
			if (ScoutUtil.isBagSlot(realIndex)) {
				return instance.quickMove(player, realIndex);
			}
		}
		return instance.quickMove(player, index);
	}

	@Shadow
	public static boolean canInsertItemIntoSlot(@Nullable Slot slot, ItemStack stack, boolean allowOverflow) {
		return false;
	}
	@Shadow
	public boolean canInsertIntoSlot(ItemStack stack, Slot slot) {
		return true;
	}
	@Shadow
	public abstract ItemStack getCursorStack();
	@Shadow
	public DefaultedList<Slot> slots;
}

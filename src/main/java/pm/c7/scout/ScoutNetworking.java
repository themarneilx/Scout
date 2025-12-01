package pm.c7.scout;

import net.minecraft.item.ItemStack;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public class ScoutNetworking {
	public static final Identifier ENABLE_SLOTS_ID = Identifier.of(ScoutUtil.MOD_ID, "enable_slots");

	public record EnableSlotsPayload(ItemStack satchel, ItemStack leftPouch, ItemStack rightPouch) implements CustomPayload {
		public static final CustomPayload.Id<EnableSlotsPayload> ID = new CustomPayload.Id<>(ENABLE_SLOTS_ID);
		public static final PacketCodec<RegistryByteBuf, EnableSlotsPayload> CODEC = PacketCodec.tuple(
			ItemStack.OPTIONAL_PACKET_CODEC, EnableSlotsPayload::satchel,
			ItemStack.OPTIONAL_PACKET_CODEC, EnableSlotsPayload::leftPouch,
			ItemStack.OPTIONAL_PACKET_CODEC, EnableSlotsPayload::rightPouch,
			EnableSlotsPayload::new
		);

		@Override
		public CustomPayload.Id<? extends CustomPayload> getId() {
			return ID;
		}
	}
}

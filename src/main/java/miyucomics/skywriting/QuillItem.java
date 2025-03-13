package miyucomics.skywriting;

import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.UseAction;
import net.minecraft.world.World;

public class QuillItem extends Item {
	private StrokeEntity stroke;

	public QuillItem() {
		super(new FabricItemSettings().maxCount(1));
	}

	@Override
	public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
		ItemStack stack = user.getStackInHand(hand);
		user.setCurrentHand(hand);
		if (world.isClient)
			return TypedActionResult.success(stack);

		stroke = new StrokeEntity(world);
		stroke.setPosition(user.getEyePos().add(user.getRotationVector()));
		world.spawnEntity(stroke);

		return TypedActionResult.success(stack);
	}

	@Override
	public void usageTick(World world, LivingEntity user, ItemStack stack, int remainingUseTicks) {
		if (world.isClient)
			return;
		stroke.appendStroke(user.getEyePos().add(user.getRotationVector()).subtract(stroke.getPos()));
	}

	@Override
	public void onStoppedUsing(ItemStack stack, World world, LivingEntity user, int remainingUseTicks) {
		if (world.isClient)
			return;
		stroke = null;
	}

	@Override
	public int getMaxUseTime(ItemStack stack) {
		return 20 * 5;
	}

	@Override
	public UseAction getUseAction(ItemStack stack) {
		return UseAction.CROSSBOW;
	}
}
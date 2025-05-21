package miyucomics.skywriting;

import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
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

		world.playSound(null, user.getX(), user.getY(), user.getZ(), SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME, SoundCategory.PLAYERS, 0.5f, 1.5f);

		return TypedActionResult.success(stack);
	}

	@Override
	public void usageTick(World world, LivingEntity user, ItemStack stack, int remainingUseTicks) {
		if (stroke == null)
			return;
		stroke.appendStroke(user.getEyePos().add(user.getRotationVector()).subtract(stroke.getPos()));
	}

	@Override
	public void onStoppedUsing(ItemStack stack, World world, LivingEntity user, int remainingUseTicks) {
		stroke = null;
		world.playSound(null, user.getX(), user.getY(), user.getZ(), SoundEvents.BLOCK_AMETHYST_BLOCK_BREAK, SoundCategory.PLAYERS, 0.5f, 1.0f);
	}

	@Override
	public int getMaxUseTime(ItemStack stack) {
		return Integer.MAX_VALUE;
	}

	@Override
	public UseAction getUseAction(ItemStack stack) {
		return UseAction.CROSSBOW;
	}
}
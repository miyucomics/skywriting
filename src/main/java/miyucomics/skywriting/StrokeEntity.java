package miyucomics.skywriting;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtFloat;
import net.minecraft.nbt.NbtList;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.EntitySpawnS2CPacket;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;

public class StrokeEntity extends Entity {
	public List<Vec3d> clientVertices = new ArrayList<>();
	public static final TrackedData<NbtCompound> strokeDataTracker = DataTracker.registerData(StrokeEntity.class, TrackedDataHandlerRegistry.NBT_COMPOUND);

	public StrokeEntity(EntityType<? extends Entity> entityType, World world) {
		super(entityType, world);
	}

	public StrokeEntity(World world) {
		this(SkywritingMain.STROKE_ENTITY, world);
	}

	public void appendStroke(Vec3d newPoint) {
		NbtCompound currentData = dataTracker.get(strokeDataTracker);
		NbtList oldList = currentData.getList("shape", NbtElement.FLOAT_TYPE);

		NbtList newList = new NbtList();
		newList.addAll(oldList);

		newList.add(NbtFloat.of((float) newPoint.x));
		newList.add(NbtFloat.of((float) newPoint.y));
		newList.add(NbtFloat.of((float) newPoint.z));

		NbtCompound newCompound = new NbtCompound();
		newCompound.put("shape", newList);

		dataTracker.set(strokeDataTracker, newCompound);
	}

	public void cacheVertices() {
		NbtList list = this.dataTracker.get(strokeDataTracker).getList("shape", NbtElement.FLOAT_TYPE);
		ArrayList<Vec3d> intermediate = new ArrayList<>();
		for (int i = 0; i < list.size() / 3; i++) {
			intermediate.add(new Vec3d(
				list.getFloat(3 * i),
				list.getFloat(3 * i + 1),
				list.getFloat(3 * i + 2)
			));
		}
		this.clientVertices = StrokeUtils.generateSmoothCurve(intermediate);
	}

	@Override
	protected float getEyeHeight(EntityPose pose, EntityDimensions dimensions) {
		return 0.25f;
	}

	@Override
	public Packet<ClientPlayPacketListener> createSpawnPacket() {
		return new EntitySpawnS2CPacket(this);
	}

	@Override
	public void readCustomDataFromNbt(NbtCompound nbt) {
		dataTracker.set(strokeDataTracker, nbt.getCompound("shape"));
	}

	@Override
	public void writeCustomDataToNbt(NbtCompound nbt) {
		nbt.put("shape", dataTracker.get(strokeDataTracker));
	}

	@Override
	protected void initDataTracker() {
		dataTracker.startTracking(strokeDataTracker, new NbtCompound());
	}

	@Override
	public void onTrackedDataSet(TrackedData<?> data) {
		if (data == strokeDataTracker) {
			cacheVertices();
		}
	}
}
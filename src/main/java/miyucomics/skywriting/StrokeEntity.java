package miyucomics.skywriting;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.nbt.*;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class StrokeEntity extends Entity {
	private static final int MAX_VERTICES = 500;
	private static final double MIN_DISTANCE = 0.03;

	@Environment(EnvType.CLIENT)
	public List<Vec3d> clientVertices = new ArrayList<>();

	private final List<Vec3d> vertices = new ArrayList<>();
	private Vec3d lastPosition;

	@Override
	public boolean shouldRender(double distance) {
		return true;
	}

	public StrokeEntity(EntityType<? extends StrokeEntity> entityType, World world) {
		super(entityType, world);
		this.noClip = true;
		this.lastPosition = this.getPos();
	}

	public StrokeEntity(World world) {
		this(SkywritingMain.STROKE_ENTITY, world);
	}

	@Override
	protected void initDataTracker() {}

	@Override
	protected void readCustomDataFromNbt(NbtCompound nbt) {
		NbtList vertexList = nbt.getList("Vertices", NbtElement.DOUBLE_TYPE);
		for (int i = 0; i < vertexList.size(); i += 3)
			vertices.add(new Vec3d(vertexList.getDouble(i), vertexList.getDouble(i + 1), vertexList.getDouble(i + 2)));
		if (this.getWorld().isClient)
			clientVertices = new ArrayList<>(vertices);
	}

	@Override
	protected void writeCustomDataToNbt(NbtCompound nbt) {
		NbtList vertexList = new NbtList();
		for (Vec3d vertex : vertices) {
			vertexList.add(NbtDouble.of(vertex.x));
			vertexList.add(NbtDouble.of(vertex.y));
			vertexList.add(NbtDouble.of(vertex.z));
		}
		nbt.put("Vertices", vertexList);
	}

	public void appendStroke(Vec3d offset) {
		Vec3d newPos = this.getPos().add(offset);
		if (newPos.squaredDistanceTo(lastPosition) >= MIN_DISTANCE * MIN_DISTANCE) {
			vertices.add(offset);
			lastPosition = newPos;
			if (vertices.size() > MAX_VERTICES)
				vertices.remove(0);
			if (!this.getWorld().isClient)
				syncWithClients();
		}
	}

	private void syncWithClients() {
		PacketByteBuf buf = PacketByteBufs.create();
		buf.writeInt(this.getId());
		buf.writeDouble(vertices.get(vertices.size() - 1).x);
		buf.writeDouble(vertices.get(vertices.size() - 1).y);
		buf.writeDouble(vertices.get(vertices.size() - 1).z);
		for (ServerPlayerEntity player : PlayerLookup.tracking(this))
			ServerPlayNetworking.send(player, SkywritingClient.STROKE_UPDATE_CHANNEL, buf);
	}

	@Override
	public Packet<ClientPlayPacketListener> createSpawnPacket() {
		PacketByteBuf buf = PacketByteBufs.create();
		buf.writeVarInt(this.getId());
		buf.writeUuid(this.getUuid());
		buf.writeDouble(this.getX());
		buf.writeDouble(this.getY());
		buf.writeDouble(this.getZ());
		buf.writeVarInt(vertices.size());
		for (Vec3d vertex : vertices) {
			buf.writeDouble(vertex.x);
			buf.writeDouble(vertex.y);
			buf.writeDouble(vertex.z);
		}
		return ServerPlayNetworking.createS2CPacket(SkywritingClient.STROKE_SPAWN_CHANNEL, buf);
	}

	@Environment(EnvType.CLIENT)
	public static void handleStrokeSpawn(MinecraftClient client, ClientPlayNetworkHandler handler, PacketByteBuf buf, PacketSender sender) {
		int entityId = buf.readVarInt();
		UUID uuid = buf.readUuid();
		double x = buf.readDouble();
		double y = buf.readDouble();
		double z = buf.readDouble();

		int vertexCount = buf.readVarInt();
		List<Vec3d> receivedVertices = new ArrayList<>();

		for (int i = 0; i < vertexCount; i++) {
			double vx = buf.readDouble();
			double vy = buf.readDouble();
			double vz = buf.readDouble();
			receivedVertices.add(new Vec3d(vx, vy, vz));
		}

		client.execute(() -> {
			ClientWorld world = client.world;
			StrokeEntity entity = new StrokeEntity(world);
			entity.setId(entityId);
			entity.setUuid(uuid);
			entity.setPos(x, y, z);
			entity.clientVertices = receivedVertices;
			world.addEntity(entityId, entity);
		});
	}

	@Environment(EnvType.CLIENT)
	public static void handleStrokeUpdate(MinecraftClient client, ClientPlayNetworkHandler handler, PacketByteBuf buf, PacketSender sender) {
		int entityId = buf.readInt();
		double x = buf.readDouble();
		double y = buf.readDouble();
		double z = buf.readDouble();
		client.execute(() -> {
			Entity entity = client.world.getEntityById(entityId);
			if (entity instanceof StrokeEntity stroke) {
				stroke.clientVertices.add(new Vec3d(x, y, z));
				if (stroke.clientVertices.size() > MAX_VERTICES)
					stroke.clientVertices.remove(0);
			}
		});
	}
}
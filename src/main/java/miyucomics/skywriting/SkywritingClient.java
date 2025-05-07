package miyucomics.skywriting;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.util.Identifier;

public class SkywritingClient implements ClientModInitializer {
	public static final Identifier STROKE_SPAWN_CHANNEL = new Identifier(SkywritingMain.MOD_ID, "stroke_spawn");
	public static final Identifier STROKE_UPDATE_CHANNEL = new Identifier(SkywritingMain.MOD_ID, "stroke_update");

	@Override
	public void onInitializeClient() {
		EntityRendererRegistry.register(SkywritingMain.STROKE_ENTITY, StrokeRenderer::new);
		ClientPlayNetworking.registerGlobalReceiver(STROKE_SPAWN_CHANNEL, StrokeEntity::handleStrokeSpawn);
		ClientPlayNetworking.registerGlobalReceiver(STROKE_UPDATE_CHANNEL, StrokeEntity::handleStrokeUpdate);
	}
}
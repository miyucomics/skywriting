package miyucomics.skywriting;

import net.fabricmc.api.ModInitializer;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public class SkywritingMain implements ModInitializer {
	public static final String MOD_ID = "skywriting";
	public static final EntityType<StrokeEntity> STROKE_ENTITY = EntityType.Builder.<StrokeEntity>create(StrokeEntity::new, SpawnGroup.MISC).setDimensions(0.01f, 0.01f).build("stroke");

	@Override
	public void onInitialize() {
		Registry.register(Registries.ENTITY_TYPE, new Identifier(MOD_ID, "stroke"), STROKE_ENTITY);
		Registry.register(Registries.ITEM, new Identifier(MOD_ID, "quill"), new QuillItem());
	}
}
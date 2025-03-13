package miyucomics.skywriting;

import net.minecraft.client.render.*;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.ColorHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;

public class StrokeRenderer extends EntityRenderer<StrokeEntity> {
	private static final int SIDES = 6;
	private static final double RADIUS = 0.01;
	private static final double ANGLE_INCREMENT = 2 * Math.PI / SIDES;

	private static final RenderLayer renderLayer = RenderLayer.getEntityCutoutNoCull(new Identifier("textures/entity/white.png"));

	public StrokeRenderer(EntityRendererFactory.Context ctx) {
		super(ctx);
	}

	@Override
	public Identifier getTexture(StrokeEntity entity) {
		return null;
	}

	@Override
	public boolean shouldRender(StrokeEntity entity, Frustum frustum, double x, double y, double z) {
		return true;
	}

	@Override
	public void render(StrokeEntity entity, float yaw, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light) {
		List<Vec3d> smoothCurve = entity.clientVertices;
		if (smoothCurve.size() < 2)
			return;

		List<Vec3d[]> rings = new ArrayList<>();
		for (int i = 0; i < smoothCurve.size(); i++) {
			Vec3d center = smoothCurve.get(i);
			Vec3d tangent;
			if (i == 0) {
				tangent = smoothCurve.get(i + 1).subtract(center).normalize();
			} else if (i == smoothCurve.size() - 1) {
				tangent = center.subtract(smoothCurve.get(i - 1)).normalize();
			} else {
				tangent = smoothCurve.get(i + 1).subtract(smoothCurve.get(i - 1)).normalize();
			}

			Vec3d normal = tangent.crossProduct(new Vec3d(0, 1, 0));
			if (normal.length() < 1e-6)
				normal = tangent.crossProduct(new Vec3d(1, 0, 0));
			normal = normal.normalize();
			Vec3d binormal = tangent.crossProduct(normal).normalize();

			Vec3d[] ring = new Vec3d[SIDES];
			for (int j = 0; j < SIDES; j++) {
				double angle = j * ANGLE_INCREMENT;
				Vec3d offset = normal.multiply(Math.cos(angle) * RADIUS)
						.add(binormal.multiply(Math.sin(angle) * RADIUS));
				ring[j] = center.add(offset);
			}
			rings.add(ring);
		}

		VertexConsumer buf = vertexConsumers.getBuffer(renderLayer);
		Matrix4f pose = matrices.peek().getPositionMatrix();
		Matrix3f normMatrix = matrices.peek().getNormalMatrix();

		for (int i = 0; i < rings.size() - 1; i++) {
			Vec3d[] ring1 = rings.get(i);
			Vec3d[] ring2 = rings.get(i + 1);
			for (int j = 0; j < SIDES; j++) {
				int next = (j + 1) % SIDES;
				vertex(pose, normMatrix, buf, ring1[j]);
				vertex(pose, normMatrix, buf, ring1[next]);
				vertex(pose, normMatrix, buf, ring2[next]);
				vertex(pose, normMatrix, buf, ring2[j]);
			}
		}
	}

	private void vertex(Matrix4f pose, Matrix3f norm, VertexConsumer vertices, Vec3d position) {
		vertices.vertex(pose, (float) position.getX(), (float) position.getY(), (float) position.getZ())
				.color(ColorHelper.Abgr.getAbgr(255, 255, 255, 255))
				.texture(0f, 0f)
				.overlay(OverlayTexture.DEFAULT_UV)
				.light(LightmapTextureManager.MAX_LIGHT_COORDINATE)
				.normal(norm, 0f, 1f, 0f)
				.next();
	}
}
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
	private static final int SIDES = 4;
	private static final double RADIUS = 0.01;
	private static final double ANGLE_INCREMENT = 2 * Math.PI / SIDES;
	private static final int CURVE_SUBDIVISIONS = 3;
	private static final int STROKE_COLOR = ColorHelper.Abgr.getAbgr(255, 255, 255, 255);
	private static final RenderLayer RENDER_LAYER = RenderLayer.getEntityCutout(new Identifier("textures/misc/white.png"));

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
		List<Vec3d> rawPoints = entity.clientVertices;
		if (rawPoints.size() < 2)
			return;

		List<Vec3d> smoothCurve = interpolateCurve(rawPoints);

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
			if (normal.lengthSquared() < 1e-6)
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

		VertexConsumer buf = vertexConsumers.getBuffer(RENDER_LAYER);
		Matrix4f pose = matrices.peek().getPositionMatrix();
		Matrix3f normMatrix = matrices.peek().getNormalMatrix();

		for (int i = 0; i < rings.size() - 1; i++) {
			Vec3d[] ring1 = rings.get(i);
			Vec3d[] ring2 = rings.get(i + 1);

			Vec3d center1 = smoothCurve.get(i);
			Vec3d center2 = smoothCurve.get(i + 1);

			for (int j = 0; j < SIDES; j++) {
				int next = (j + 1) % SIDES;

				Vec3d normal1 = ring1[j].subtract(center1).normalize();
				Vec3d normal2 = ring2[j].subtract(center2).normalize();
				Vec3d normal3 = ring2[next].subtract(center2).normalize();
				Vec3d normal4 = ring1[next].subtract(center1).normalize();

				vertex(pose, normMatrix, buf, ring1[j], normal1);
				vertex(pose, normMatrix, buf, ring1[next], normal4);
				vertex(pose, normMatrix, buf, ring2[next], normal3);
				vertex(pose, normMatrix, buf, ring2[j], normal2);
			}
		}
	}

	private List<Vec3d> interpolateCurve(List<Vec3d> points) {
		if (points.size() < 2) {
			return new ArrayList<>(points);
		}

		List<Vec3d> result = new ArrayList<>();

		result.add(points.get(0));

		for (int i = 0; i < points.size() - 1; i++) {
			Vec3d p0 = i > 0 ? points.get(i - 1) : points.get(i);
			Vec3d p1 = points.get(i);
			Vec3d p2 = points.get(i + 1);
			Vec3d p3 = (i + 2 < points.size()) ? points.get(i + 2) : p2.add(p2.subtract(p1));

			for (int j = 1; j <= CURVE_SUBDIVISIONS; j++) {
				double t = (double) j / (CURVE_SUBDIVISIONS + 1);
				result.add(catmullRomInterpolate(p0, p1, p2, p3, t));
			}

			if (i < points.size() - 2)
				result.add(points.get(i + 1));
		}

		result.add(points.get(points.size() - 1));
		return result;
	}

	private Vec3d catmullRomInterpolate(Vec3d p0, Vec3d p1, Vec3d p2, Vec3d p3, double t) {
		double t2 = t * t;
		double t3 = t2 * t;

		double b0 = -0.5 * t3 + t2 - 0.5 * t;
		double b1 = 1.5 * t3 - 2.5 * t2 + 1.0;
		double b2 = -1.5 * t3 + 2.0 * t2 + 0.5 * t;
		double b3 = 0.5 * t3 - 0.5 * t2;

		return new Vec3d(
				b0 * p0.x + b1 * p1.x + b2 * p2.x + b3 * p3.x,
				b0 * p0.y + b1 * p1.y + b2 * p2.y + b3 * p3.y,
				b0 * p0.z + b1 * p1.z + b2 * p2.z + b3 * p3.z
		);
	}

	private void vertex(Matrix4f pose, Matrix3f norm, VertexConsumer vertices, Vec3d position, Vec3d normal) {
		vertices.vertex(pose, (float) position.getX(), (float) position.getY(), (float) position.getZ())
				.color(STROKE_COLOR)
				.texture(0f, 0f)
				.overlay(OverlayTexture.DEFAULT_UV)
				.light(LightmapTextureManager.MAX_LIGHT_COORDINATE)
				.normal(norm, (float) normal.x, (float) normal.y, (float) normal.z)
				.next();
	}
}
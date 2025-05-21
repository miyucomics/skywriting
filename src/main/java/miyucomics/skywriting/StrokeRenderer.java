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
		if (entity.clientVertices.size() < 2)
			return;

		List<Vec3d> smooth = sampleCatmullRom(entity.clientVertices, 8);

		VertexConsumer buf = vertexConsumers.getBuffer(RENDER_LAYER);
		Matrix4f pose = matrices.peek().getPositionMatrix();
		Matrix3f normalMatrix = matrices.peek().getNormalMatrix();
		List<Vec3d[]> rings = new ArrayList<>();

		for (int i = 0; i < smooth.size(); i++) {
			Vec3d center = smooth.get(i);
			Vec3d tangent;
			if (i == 0) {
				tangent = smooth.get(i + 1).subtract(center).normalize();
			} else if (i == smooth.size() - 1) {
				tangent = center.subtract(smooth.get(i - 1)).normalize();
			} else {
				tangent = smooth.get(i + 1).subtract(smooth.get(i - 1)).normalize();
			}

			Vec3d normal = tangent.crossProduct(new Vec3d(0, 1, 0));
			if (normal.length() < 1e-6)
				normal = tangent.crossProduct(new Vec3d(1, 0, 0));
			normal = normal.normalize();
			Vec3d binormal = tangent.crossProduct(normal).normalize();

			Vec3d[] ring = new Vec3d[3];
			for (int j = 0; j < 3; j++) {
				double angle = j * ANGLE_INCREMENT;
				Vec3d offset = normal.multiply(Math.cos(angle) * RADIUS)
						.add(binormal.multiply(Math.sin(angle) * RADIUS));
				ring[j] = center.add(offset);
			}
			rings.add(ring);
		}

		for (int i = 0; i < rings.size() - 1; i++) {
			Vec3d[] ring1 = rings.get(i);
			Vec3d[] ring2 = rings.get(i + 1);
			for (int j = 0; j < 3; j++) {
				int next = (j + 1) % 3;
				vertex(pose, normalMatrix, buf, ring1[j]);
				vertex(pose, normalMatrix, buf, ring1[next]);
				vertex(pose, normalMatrix, buf, ring2[next]);
				vertex(pose, normalMatrix, buf, ring2[j]);
			}
		}
	}

	private void vertex(Matrix4f pose, Matrix3f norm, VertexConsumer vertices, Vec3d position) {
		vertices.vertex(pose, (float) position.getX(), (float) position.getY(), (float) position.getZ())
			.color(STROKE_COLOR)
			.texture(0f, 0f)
			.overlay(OverlayTexture.DEFAULT_UV)
			.light(LightmapTextureManager.MAX_LIGHT_COORDINATE)
			.normal(norm, 0f, 1f, 0f)
			.next();
	}

	private List<Vec3d> sampleCatmullRom(List<Vec3d> pts, int samplesPerSegment) {
		List<Vec3d> out = new ArrayList<>();
		if (pts.size() < 4) return pts;  // need at least 4 for CR
		for (int i = 0; i < pts.size() - 3; i++) {
			Vec3d P0 = pts.get(i), P1 = pts.get(i+1), P2 = pts.get(i+2), P3 = pts.get(i+3);
			for (int s = 0; s < samplesPerSegment; s++) {
				double t = s / (double) samplesPerSegment;
				out.add(CR(P0, P1, P2, P3, t));
			}
		}
		// ensure last original point appears
		out.add(pts.get(pts.size()-2));
		out.add(pts.get(pts.size()-1));
		return out;
	}

	Vec3d CR(Vec3d P0, Vec3d P1, Vec3d P2, Vec3d P3, double t) {
		double t2 = t*t, t3 = t2*t;
		return P1.multiply(2)
				.add(P2.subtract(P0).multiply(t))
				.add(P0.multiply(2).subtract(P1.multiply(5)).add(P2.multiply(4)).subtract(P3).multiply(t2))
				.add(P3.subtract(P0).add(P1.multiply(3).subtract(P2.multiply(3))).multiply(t3))
				.multiply(0.5);
	}
}
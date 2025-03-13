package miyucomics.skywriting;

import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.List;

public class StrokeUtils {
	private static final int SAMPLES_PER_SEGMENT = 2;

	public static ArrayList<Vec3d> optimizeClosePoints(ArrayList<Vec3d> points, double minDistance) {
		if (points == null || points.size() < 2) {
			return points;
		}

		ArrayList<Vec3d> filtered = new ArrayList<>();
		Vec3d lastAccepted = points.get(0);
		filtered.add(lastAccepted);

		for (int i = 1; i < points.size(); i++) {
			Vec3d current = points.get(i);
			if (current.subtract(lastAccepted).length() >= minDistance) {
				filtered.add(current);
				lastAccepted = current;
			}
		}

		return filtered;
	}

	public static List<Vec3d> generateSmoothCurve(List<Vec3d> points) {
		List<Vec3d> smoothPoints = new ArrayList<>();

		for (int i = 0; i < points.size() - 1; i++) {
			Vec3d p0 = (i == 0) ? points.get(i) : points.get(i - 1);
			Vec3d p1 = points.get(i);
			Vec3d p2 = points.get(i + 1);
			Vec3d p3 = (i + 2 < points.size()) ? points.get(i + 2) : points.get(i + 1);

			for (int j = 0; j < SAMPLES_PER_SEGMENT; j++) {
				float t = j / (float) SAMPLES_PER_SEGMENT;
				smoothPoints.add(catmullRom(p0, p1, p2, p3, t));
			}
		}

		smoothPoints.add(points.get(points.size() - 1));
		return smoothPoints;
	}

	private static Vec3d catmullRom(Vec3d p0, Vec3d p1, Vec3d p2, Vec3d p3, float t) {
		float t2 = t * t;
		float t3 = t2 * t;
		double x = 0.5 * ((2 * p1.getX()) +
				(-p0.getX() + p2.getX()) * t +
				(2 * p0.getX() - 5 * p1.getX() + 4 * p2.getX() - p3.getX()) * t2 +
				(-p0.getX() + 3 * p1.getX() - 3 * p2.getX() + p3.getX()) * t3);
		double y = 0.5 * ((2 * p1.getY()) +
				(-p0.getY() + p2.getY()) * t +
				(2 * p0.getY() - 5 * p1.getY() + 4 * p2.getY() - p3.getY()) * t2 +
				(-p0.getY() + 3 * p1.getY() - 3 * p2.getY() + p3.getY()) * t3);
		double z = 0.5 * ((2 * p1.getZ()) +
				(-p0.getZ() + p2.getZ()) * t +
				(2 * p0.getZ() - 5 * p1.getZ() + 4 * p2.getZ() - p3.getZ()) * t2 +
				(-p0.getZ() + 3 * p1.getZ() - 3 * p2.getZ() + p3.getZ()) * t3);
		return new Vec3d(x, y, z);
	}
}

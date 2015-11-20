package com.mygdx.game.utilities;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;

/**
 * @author jsjolund
 */
public class GeometryUtils {

	/**
	 * Projects a point to a line segment.
	 *
	 * @param nearest Output for the nearest vector to the segment.
	 * @param start Segment start point
	 * @param end Segment end point
	 * @param point Point to project from
	 * @return Squared distance between point and nearest.
	 */
	public static float nearestSegmentPointSquareDistance
	(Vector3 nearest, Vector3 start, Vector3 end, Vector3 point) {
		nearest.set(start);
		float abX = end.x - start.x;
		float abY = end.y - start.y;
		float abZ = end.z - start.z;
		float abLen2 = abX * abX + abY * abY + abZ * abZ;
		if (abLen2 > 0) { // Avoid NaN due to the indeterminate form 0/0
			float t = ((point.x - start.x) * abX + (point.y - start.y) * abY + (point.z - start.z) * abZ) / abLen2;
			float s = MathUtils.clamp(t, 0, 1);
			nearest.x += abX * s;
			nearest.y += abY * s;
			nearest.z += abZ * s;
		}
		return nearest.dst2(point);
	}


}


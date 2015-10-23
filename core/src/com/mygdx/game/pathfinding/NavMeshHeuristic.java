package com.mygdx.game.pathfinding;

import com.badlogic.gdx.ai.pfa.Heuristic;
import com.badlogic.gdx.math.Vector3;

/**
 * Created by Johannes Sjolund on 9/27/15.
 */
public class NavMeshHeuristic implements Heuristic<Triangle> {

	@Override
	public float estimate(Triangle node, Triangle endNode) {
		float minDst = Float.MAX_VALUE;
		for (int i = 0; i < node.corners.size; i++) {
			Vector3 a = node.corners.get(i);
			for (int j = 0; j < endNode.corners.size; j++) {
				Vector3 b = endNode.corners.get(j);
				float dst = a.dst(b);
				if (dst < minDst) {
					minDst = dst;
				}
			}
		}
		return minDst;
	}
}

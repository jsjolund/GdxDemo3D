package com.mygdx.game.navmesh;

import com.badlogic.gdx.ai.pfa.Heuristic;

/**
 * Created by Johannes Sjolund on 9/27/15.
 */
public class NavMeshHeuristic implements Heuristic<Triangle> {
	@Override
	public float estimate(Triangle node, Triangle endNode) {
		return node.centroid.dst2(endNode.centroid);
	}
}

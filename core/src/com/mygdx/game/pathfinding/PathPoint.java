package com.mygdx.game.pathfinding;

import com.badlogic.gdx.math.Vector3;

/**
 * Created by Johannes Sjolund on 10/20/15.
 */
public class PathPoint {
	/**
	 * Point where the path crosses a navmesh edge
	 */
	public Vector3 edgeCrossingPoint;
	/**
	 * Index of the navmesh triangle which the path enters past the crossing point
	 */
	public int crossedTriIndex;

	@Override
	public String toString() {
		return String.format("%s %s",crossedTriIndex, edgeCrossingPoint);
	}

	public PathPoint(Vector3 edgeCrossingPoint, int crossedTriIndex) {
		this.edgeCrossingPoint = edgeCrossingPoint;
		this.crossedTriIndex = crossedTriIndex;
	}
}

package com.mygdx.game.pathfinding;

import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;

/**
 * Created by Johannes Sjolund on 10/20/15.
 */
public class PathPoint {
	/**
	 * World coordinates of the path point.
	 */
	public Vector3 point = new Vector3();
	/**
	 * Index of the triangle which must be crossed to reach the next path point.
	 */
	public int crossingTriangle = -1;
	/**
	 * Path edges connected to this point. Can be used for spline generation at some point...
	 */
	public Array<Edge> connectingEdges = new Array<Edge>();

	public PathPoint(Vector3 point, int crossingTriangle) {
		this.point.set(point);
		this.crossingTriangle = crossingTriangle;
	}

}

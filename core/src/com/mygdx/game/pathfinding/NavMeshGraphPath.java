package com.mygdx.game.pathfinding;

import com.badlogic.gdx.ai.pfa.Connection;
import com.badlogic.gdx.ai.pfa.DefaultGraphPath;
import com.badlogic.gdx.math.Vector3;

/**
 * Created by Johannes Sjolund on 9/26/15.
 */
public class NavMeshGraphPath extends DefaultGraphPath<Connection<Triangle>> {
	/**
	 * The start point when generating a point path for this triangle path
	 */
	public Vector3 start;
	/**
	 * The end point when generating a point path for this triangle path
	 */
	public Vector3 end;
	/**
	 * If the triangle path is empty, the point path will span this triangle
	 */
	public Triangle startTri;
}
